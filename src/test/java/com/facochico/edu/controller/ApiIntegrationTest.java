package com.facochico.edu.controller;

import com.facochico.edu.testutil.WithPostgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static java.time.temporal.ChronoUnit.DAYS;
import static lombok.AccessLevel.PACKAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithPostgres
@RequiredArgsConstructor(access = PACKAGE, onConstructor_ = @Autowired)
class ApiIntegrationTest {
    private final MockMvc mvc;
    private final JsonMapper json;
    private final Clock clock;

    @Test
    @SneakyThrows
    void shouldReadProfileAtLocationWhenUserIsCreated() {
        final var payload = Map.of(
            "name", "  Гришаня  ", "email", " GRISHA.HTTP@Example.org ", "role", "STUDENT",
            "profile", Map.of("bio", "Учусь на третьем курсе")
        );

        final var response = sendJson(post("/api/v1/users"), payload)
            .andExpect(status().isCreated())
            .andExpect(header().exists(LOCATION))
            .andReturn().getResponse();
        final var location = response.getHeader(LOCATION);

        mvc.perform(get(location))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.details.name").value("Гришаня"))
            .andExpect(jsonPath("$.details.email").value("grisha.http@example.org"))
            .andExpect(jsonPath("$.details.profile.bio").value("Учусь на третьем курсе"));
    }

    @Test
    @SneakyThrows
    void shouldListOnlyStudentSummariesWhenRoleFilterIsProvided() {
        final var studentId = createUser("Аня", "anya.list-http@example.org", "STUDENT");
        final var teacherId = createUser("Таня", "tanya.list-http@example.org", "TEACHER");

        final var response = mvc.perform(get("/api/v1/users").param("role", "STUDENT").param("size", "100"));

        final var body = json.readTree(response.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        assertThat(body.findValues("id").stream().map(JsonNode::longValue)).contains(studentId).doesNotContain(teacherId);
        assertThat(body.findValues("role").stream().map(JsonNode::asString)).containsOnly("STUDENT");
        assertThat(body.findValues("email")).isEmpty();
    }

    @Test
    @SneakyThrows
    void shouldReadCourseStructureWhenModuleAndLessonAreCreated() {
        final var fixture = createCourse("program");

        final var response = mvc.perform(get("/api/v1/courses/{id}", fixture.courseId()));

        response.andExpect(status().isOk())
            .andExpect(jsonPath("$.category.name").value("Программирование"))
            .andExpect(jsonPath("$.tags[0].name").value("Практика"))
            .andExpect(jsonPath("$.modules[0].details.title").value("Коллекции"))
            .andExpect(jsonPath("$.modules[0].lessons[0].details.title").value("Списки"));
    }

    @Test
    @SneakyThrows
    void shouldListStudentAndCourseWhenEnrollmentIsCreated() {
        final var fixture = createCourse("enroll");

        final var response = mvc.perform(post("/api/v1/courses/{id}/enrollments", fixture.courseId())
                .header("X-User-Id", fixture.studentId()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn().getResponse();

        mvc.perform(get(response.getHeader(LOCATION)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value(fixture.studentId()));
        mvc.perform(get("/api/v1/courses/{id}/students", fixture.courseId()).param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Мишаня"));
        mvc.perform(get("/api/v1/users/{id}/courses", fixture.studentId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(fixture.courseId()));
    }

    @Test
    @SneakyThrows
    void shouldReturnGradeWhenTeacherChecksSubmittedHomework() {
        final var fixture = createCourse("homework");
        enroll(fixture);
        final var assignment = createAssignment(fixture);
        final var work = created(sendJson(
            post("/api/v1/assignments/{id}/submissions", assignment).header("X-User-Id", fixture.studentId()),
            Map.of("content", "Проверяю пустой список")
        ));
        final var submissionId = work.path("id").longValue();

        final var graded = sendJson(
            put("/api/v1/submissions/{id}/grade", submissionId).header("X-User-Id", fixture.teacherId()),
            Map.of("score", 0, "feedback", "Нужно дописать решение")
        );

        graded.andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("GRADED"))
            .andExpect(jsonPath("$.score").value(0));
        mvc.perform(get("/api/v1/submissions").header("X-User-Id", fixture.studentId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].feedback").value("Нужно дописать решение"));
    }

    @Test
    @SneakyThrows
    void shouldReturnQuizResultWhenStudentSubmitsAnswer() {
        final var fixture = createCourse("quiz");
        enroll(fixture);
        final var quiz = created(sendJson(
            post("/api/v1/modules/{id}/quizzes", fixture.moduleId()).header("X-User-Id", fixture.teacherId()),
            Map.of("title", "Проверка знаний", "questions", List.of(Map.of(
                "text", "Какая команда читает строки?",
                "options", List.of(Map.of("text", "SELECT", "correct", true), Map.of("text", "DELETE", "correct", false))
            )))
        ));
        final var studentView = mvc.perform(
                get("/api/v1/modules/{id}/quiz", fixture.moduleId()).header("X-User-Id", fixture.studentId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$..correct").isEmpty())
            .andReturn().getResponse();
        final var question = json.readTree(studentView.getContentAsByteArray()).path("questions").path(0);
        final var choices = Map.of(question.path("id").longValue(), question.path("options").path(0).path("id").longValue());

        final var result = sendJson(
            post("/api/v1/quizzes/{id}/results", quiz.path("id").longValue()).header("X-User-Id", fixture.studentId()),
            Map.of("answers", choices)
        );

        final var location = result.andExpect(status().isCreated())
            .andExpect(jsonPath("$.score").value(1))
            .andExpect(jsonPath("$.maxScore").value(1))
            .andReturn().getResponse().getHeader(LOCATION);
        mvc.perform(get(location).header("X-User-Id", fixture.studentId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value(fixture.studentId()));
        mvc.perform(get(location).header("X-User-Id", fixture.teacherId()))
            .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void shouldDenyReadingQuizResultWhenItBelongsToAnotherStudent() {
        final var fixture = createCourse("result-owner");
        enroll(fixture);
        final var otherId = createUser("Аня", "anya.result-owner@example.org", "STUDENT");
        final var quizId = created(sendJson(
            post("/api/v1/modules/{id}/quizzes", fixture.moduleId()).header("X-User-Id", fixture.teacherId()),
            Map.of("title", "Проверка знаний", "questions", List.of(Map.of(
                "text", "Как удалить строку?",
                "options", List.of(Map.of("text", "DELETE", "correct", true), Map.of("text", "SELECT", "correct", false))
            )))
        )).path("id").longValue();
        final var questionResponse = mvc.perform(
                get("/api/v1/modules/{id}/quiz", fixture.moduleId()).header("X-User-Id", fixture.studentId()))
            .andExpect(status().isOk())
            .andReturn().getResponse();
        final var question = json.readTree(questionResponse.getContentAsByteArray()).path("questions").path(0);
        final var result = created(sendJson(
            post("/api/v1/quizzes/{id}/results", quizId).header("X-User-Id", fixture.studentId()),
            Map.of("answers", Map.of(question.path("id").longValue(), question.path("options").path(0).path("id").longValue()))
        ));

        final var response = mvc.perform(get("/api/v1/quiz-results/{id}", result.path("id").longValue())
            .header("X-User-Id", otherId));

        response.andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }

    @Test
    @SneakyThrows
    void shouldUpdateAndDeleteReviewWhenRequestedByAuthor() {
        final var fixture = createCourse("review");
        final var review = created(sendJson(
            post("/api/v1/courses/{id}/reviews", fixture.courseId()).header("X-User-Id", fixture.studentId()),
            Map.of("rating", 4, "comment", "Понравились задачи")
        ));
        final var id = review.path("id").longValue();
        final var updated = Map.of("rating", 5, "comment", "Разобрался со списками");

        final var response = sendJson(put("/api/v1/reviews/{id}", id).header("X-User-Id", fixture.studentId()), updated);

        response.andExpect(status().isOk()).andExpect(jsonPath("$.details.rating").value(5));
        mvc.perform(delete("/api/v1/reviews/{id}", id).header("X-User-Id", fixture.studentId()))
            .andExpect(status().isNoContent());
    }

    @Test
    @SneakyThrows
    void shouldRejectReviewUpdateWhenRequestedByAnotherStudent() {
        final var fixture = createCourse("review-owner");
        final var other = createUser("Аня", "anya.review-http@example.org", "STUDENT");
        final var review = created(sendJson(
            post("/api/v1/courses/{id}/reviews", fixture.courseId()).header("X-User-Id", fixture.studentId()),
            Map.of("rating", 4)
        ));
        final var id = review.path("id").longValue();

        final var response = sendJson(put("/api/v1/reviews/{id}", id).header("X-User-Id", other), Map.of("rating", 1));

        response.andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Only the author may modify the review"));
        mvc.perform(get("/api/v1/reviews/{id}", id)).andExpect(jsonPath("$.details.rating").value(4));
    }

    @Test
    @SneakyThrows
    void shouldReturnFieldErrorsWhenUserRequestIsInvalid() {
        final var payload = Map.of("name", "", "email", "anya@example.org");

        final var response = sendJson(post("/api/v1/users"), payload);

        response.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errors.name").isString())
            .andExpect(jsonPath("$.errors.role").isString());
    }

    @Test
    @SneakyThrows
    void shouldReturnValidationProblemWhenCourseTitleIsBlank() {
        final var payload = Map.of("title", " ", "categoryId", 1, "teacherId", 1);

        final var response = sendJson(post("/api/v1/courses"), payload);

        response.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Course title is required"));
    }

    @Test
    @SneakyThrows
    void shouldReturnBadRequestWhenPageSizeExceedsLimit() {
        final var request = get("/api/v1/courses").param("size", "101");

        final var response = mvc.perform(request);

        response.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errors.size").isString());
    }

    @Test
    @SneakyThrows
    void shouldReturnInvalidBodyProblemWhenJsonCannotBeRead() {
        final var request = post("/api/v1/users").contentType(APPLICATION_JSON).content("{");

        final var response = mvc.perform(request);

        response.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Request body must contain valid JSON with the expected fields"));
    }

    @Test
    @SneakyThrows
    void shouldReturnBadRequestWhenActorHeaderIsMissing() {
        final var request = get("/api/v1/submissions");

        final var response = mvc.perform(request);

        response.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }

    @Test
    @SneakyThrows
    void shouldReturnNotFoundWhenCourseDoesNotExist() {
        final var request = get("/api/v1/courses/{id}", Long.MAX_VALUE);

        final var response = mvc.perform(request);

        response.andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @SneakyThrows
    void shouldReturnConflictWhenStudentRepeatsEnrollment() {
        final var fixture = createCourse("duplicate");
        enroll(fixture);

        final var response = mvc.perform(post("/api/v1/courses/{id}/enrollments", fixture.courseId())
            .header("X-User-Id", fixture.studentId()));

        response.andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }

    @Test
    @SneakyThrows
    void shouldReturnConflictWhenCourseWithEnrollmentsIsDeleted() {
        final var fixture = createCourse("protected");
        enroll(fixture);

        final var response = mvc.perform(delete("/api/v1/courses/{id}", fixture.courseId()));

        response.andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }

    @SneakyThrows
    private long createUser(String name, String email, String role) {
        final var request = Map.of("name", name, "email", email, "role", role);
        return created(sendJson(post("/api/v1/users"), request)).path("id").longValue();
    }

    @SneakyThrows
    private CourseFixture createCourse(String suffix) {
        final var teacherId = createUser("Таня", "tanya." + suffix + ".http@example.org", "TEACHER");
        final var studentId = createUser("Мишаня", "misha." + suffix + ".http@example.org", "STUDENT");
        final var category = created(sendJson(post("/api/v1/categories"), Map.of("name", "Программирование")));
        final var tag = created(sendJson(post("/api/v1/tags"), Map.of("name", "Практика")));
        final var course = created(sendJson(post("/api/v1/courses"), Map.of(
            "title", "Практика Java", "teacherId", teacherId, "categoryId", category.path("id").longValue(),
            "tagIds", List.of(tag.path("id").longValue())
        )));
        final var courseId = course.path("id").longValue();
        final var module = created(sendJson(post("/api/v1/courses/{id}/modules", courseId),
            Map.of("title", "Коллекции", "orderIndex", 0)));
        final var moduleId = module.path("id").longValue();
        final var lesson = created(sendJson(post("/api/v1/modules/{id}/lessons", moduleId),
            Map.of("title", "Списки", "orderIndex", 0)));
        return new CourseFixture(courseId, moduleId, lesson.path("id").longValue(), teacherId, studentId);
    }

    @SneakyThrows
    private void enroll(CourseFixture fixture) {
        mvc.perform(post("/api/v1/courses/{id}/enrollments", fixture.courseId()).header("X-User-Id", fixture.studentId()))
            .andExpect(status().isCreated());
    }

    @SneakyThrows
    private long createAssignment(CourseFixture fixture) {
        final var deadline = Instant.now(clock).plus(1, DAYS);
        return created(sendJson(
            post("/api/v1/lessons/{id}/assignments", fixture.lessonId()).header("X-User-Id", fixture.teacherId()),
            Map.of("title", "Задача про списки", "dueAt", deadline.toString(), "maxScore", 10)
        )).path("id").longValue();
    }

    @SneakyThrows
    private ResultActions sendJson(MockHttpServletRequestBuilder request, Object payload) {
        return mvc.perform(request.contentType(APPLICATION_JSON).content(json.writeValueAsBytes(payload)));
    }

    @SneakyThrows
    private JsonNode created(ResultActions actions) {
        final var response = actions.andExpect(status().isCreated()).andExpect(header().exists(LOCATION))
            .andReturn().getResponse();
        return json.readTree(response.getContentAsByteArray());
    }

    private record CourseFixture(long courseId, long moduleId, long lessonId, long teacherId, long studentId) {
    }
}
