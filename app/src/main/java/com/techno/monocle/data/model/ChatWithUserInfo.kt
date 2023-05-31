package com.techno.monocle.data.model

import com.techno.monocle.data.db.entity.Chat
import com.techno.monocle.data.db.entity.UserInfo

data class ChatWithUserInfo(
    var mChat: Chat,
    var mUserInfo: UserInfo
)
