package com.newsfeed;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 클린 아키텍처 의존성 규칙을 강제한다.
 *
 * <p>규칙이 지키려는 것:
 * <ul>
 *   <li>계층 방향은 항상 바깥 → 안 (adapter → application → domain)</li>
 *   <li>domain은 프레임워크(Spring/JPA)를 모른다</li>
 *   <li>컨텍스트 간에는 상대의 application.port(공개 계약)만 참조할 수 있다
 *       — 이 경계가 지켜져야 나중에 컨텍스트를 마이크로서비스로 추출할 수 있다</li>
 * </ul>
 */
@AnalyzeClasses(packages = "com.newsfeed", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] CONTEXTS = {"user", "post", "engagement", "fanout", "feed"};

    @ArchTest
    static final ArchRule domain_은_바깥_계층에_의존하지_않는다 =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_은_adapter_에_의존하지_않는다 =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_은_프레임워크에_의존하지_않는다 =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta.persistence..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule common_은_컨텍스트에_의존하지_않는다 =
            noClasses().that().resideInAPackage("com.newsfeed.common..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.newsfeed.user..", "com.newsfeed.post..", "com.newsfeed.engagement..",
                            "com.newsfeed.fanout..", "com.newsfeed.feed..")
                    .allowEmptyShould(true);

    /**
     * 컨텍스트 간에는 상대의 application.port.in(공개 유스케이스)만 참조할 수 있다.
     * domain, adapter, application.service는 물론 application.port.out도 막는다 —
     * port.out은 그 컨텍스트 자신의 어댑터가 구현하기 위한 내부 계약이지, 다른 컨텍스트가
     * 가져다 쓰라고 만든 것이 아니기 때문이다 (fanout이 user.application.port.in의
     * GetFollowCountsUseCase/GetFollowerIdsUseCase만 참조하는 것이 올바른 예).
     */
    @ArchTest
    static void 컨텍스트는_다른_컨텍스트의_내부에_접근하지_않는다(JavaClasses classes) {
        for (String from : CONTEXTS) {
            for (String to : CONTEXTS) {
                if (from.equals(to)) {
                    continue;
                }
                noClasses().that().resideInAPackage("com.newsfeed." + from + "..")
                        .should().dependOnClassesThat().resideInAnyPackage(
                                "com.newsfeed." + to + ".domain..",
                                "com.newsfeed." + to + ".adapter..",
                                "com.newsfeed." + to + ".application.service..",
                                "com.newsfeed." + to + ".application.port.out..")
                        .allowEmptyShould(true)
                        .check(classes);
            }
        }
    }
}
