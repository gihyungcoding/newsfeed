package com.newsfeed.user.application.port.in;

import com.newsfeed.user.domain.User;

/** 유스케이스 인터페이스(인바운드 포트). 웹 어댑터가 이 인터페이스에만 의존한다. */
public interface CreateUserUseCase {

    User create(Command command);

    record Command(String username, String displayName) {
    }
}
