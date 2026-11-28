package com.facochico.edu.controller.user;

import com.facochico.edu.controller.dto.PageQuery;
import com.facochico.edu.controller.user.dto.UserRequest;
import com.facochico.edu.domain.user.User;
import com.facochico.edu.domain.user.UserRole;
import com.facochico.edu.domain.user.dto.UserSummary;
import com.facochico.edu.service.user.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
class UserController {
    private final UserService users;

    @PostMapping
    ResponseEntity<User> create(@RequestBody @Valid UserRequest request) {
        final var user = users.create(request.toUserDetails());
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.getId())).body(user);
    }

    @GetMapping
    List<UserSummary> list(@RequestParam(required = false) UserRole role, @ModelAttribute @Valid PageQuery page) {
        return users.listUsers(role, page.page(), page.size());
    }

    @GetMapping("/{id}")
    User findById(@PathVariable @Positive long id) {
        return users.findById(id);
    }

    @PutMapping("/{id}")
    User update(@PathVariable @Positive long id, @RequestBody @Valid UserRequest request) {
        return users.update(id, request.toUserDetails());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    void delete(@PathVariable @Positive long id) {
        users.delete(id);
    }
}
