package com.callassistant.data.db

import androidx.room.TypeConverter
import com.callassistant.data.entity.CallType
import com.callassistant.data.entity.ContactSource
import com.callassistant.data.entity.RuleType
import com.callassistant.data.entity.SmsDirection

class Converters {
    @TypeConverter
    fun fromContactSource(value: ContactSource): String = value.name

    @TypeConverter
    fun toContactSource(value: String): ContactSource = ContactSource.valueOf(value)

    @TypeConverter
    fun fromCallType(value: CallType): String = value.name

    @TypeConverter
    fun toCallType(value: String): CallType = CallType.valueOf(value)

    @TypeConverter
    fun fromSmsDirection(value: SmsDirection): String = value.name

    @TypeConverter
    fun toSmsDirection(value: String): SmsDirection = SmsDirection.valueOf(value)

    @TypeConverter
    fun fromRuleType(value: RuleType): String = value.name

    @TypeConverter
    fun toRuleType(value: String): RuleType = RuleType.valueOf(value)
}
