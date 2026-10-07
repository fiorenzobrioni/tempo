package com.callbackdev.tempo.core.data.settings

import com.callbackdev.tempo.core.model.CalendarChoice
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalendarChoiceCodecTest {
    @Test
    fun `a choice reads back as written, separators and accents included`() {
        val choice =
            CalendarChoice(12, "com.google", "lucia|rossi@example.com", "Compleanni · Famiglia 🎂", shown = true)
        assertThat(CalendarChoiceCodec.decode(CalendarChoiceCodec.encode(choice))).isEqualTo(choice)
    }

    @Test
    fun `a line it cannot read is dropped`() {
        assertThat(CalendarChoiceCodec.decode("2|shown|1|a|b|c")).isNull()
        assertThat(CalendarChoiceCodec.decode("1|maybe|1|a|b|c")).isNull()
        assertThat(CalendarChoiceCodec.decode("1|shown|x|a|b|c")).isNull()
        assertThat(CalendarChoiceCodec.decode("garbage")).isNull()
    }
}
