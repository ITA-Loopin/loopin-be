package com.loopone.loopinbe.domain.loop.loopReport.messages;

public final class ReportMessages {
    private ReportMessages() {}

    public static final String GOOD = """
        루핑이님,
        최근 루프가 안정적으로 이어지고 있어요!
        이 흐름, 그대로 이어가도 좋아요.
        """.stripTrailing();

    public static final String OK = """
        루핑이님,
        완벽하진 않았지만, 루프가 이어지고 있어요.
        지금 상태로도 충분하지만, 조금 채워봐요!
        """.stripTrailing();

    public static final String HARD = """
        루핑이님,
        요즘 루프가 조금 버겁게 느껴졌을 수 있어요.
        지금보다 가벼운 루프를 만들어볼까요?
        """.stripTrailing();

    public static final String NONE = """
        루핑이님,
        최근에는 루프가 설정되지 않았어요.
        루프를 추가하러 가볼까요?
        """.stripTrailing();
}

