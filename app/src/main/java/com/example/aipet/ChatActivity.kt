        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(50)))
        root.addView(composer, matchWrap())

        setContentView(root)
        updateModeLabel()
    }

    private fun updateModeLabel() {
        modeLabel.text = if (mode == PetChatEngine.Mode.LOCAL) {
            "当前模式：免费本地陪聊（固定规则回复，不调用网络）"
        } else {
            "当前模式：独立 AI 接口（消息将发送给你配置的服务商）"
        }
        refreshFriendStatus()
    }

    private fun openChatGptWeb() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/")))
        } catch (_: Exception) {
            Toast.makeText(this, "无法打开浏览器，请检查系统应用", Toast.LENGTH_LONG).show()
        }
    }

    private fun selectApiMode() {
        if (ApiKeyStore.load(this).isNullOrBlank()) {
            openedSettingsForApi = true
            startActivity(Intent(this, AiSettingsActivity::class.java))
            Toast.makeText(this, "先配置接口地址、模型和 API Key", Toast.LENGTH_LONG).show()
        } else {
            mode = PetChatEngine.Mode.API
            history.clear() // Only new messages from this API session become provider context.
            updateModeLabel()
        }
    }

    private fun openSettings() {
        openedSettingsForApi = true
        startActivity(Intent(this, AiSettingsActivity::class.java))
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        input.setText("")
        addMessage("你", text, true)
        PetChatStore.append(this, "user", text)
        history.add(JSONObject().put("role", "user").put("content", text))
        PetAnimator.reactToText(text)
        addMessage("小伙伴", "正在思考…", false, typing = true)
        PetChatEngine.requestReply(this, text, mode, history.dropLast(1).takeLast(10)) { result ->
            removeLastTypingIndicator()
            addMessage("小伙伴", result.reply, false)
            PetChatStore.append(this, "assistant", result.reply)
            history.add(JSONObject().put("role", "assistant").put("content", result.reply))
            refreshFriendStatus()
            if (!result.ok && mode == PetChatEngine.Mode.API) {
                mode = PetChatEngine.Mode.LOCAL
                updateModeLabel()
            }
            PetAnimator.reactToText(result.reply, isReply = true)
        }
    }


    private fun refreshFriendStatus() {
        val stats = PetStateStore.get(this)
        friendStatusLabel.text = "关系：${PetStateStore.relationshipTitle(stats.affection)}  ·  好感 ${stats.affection}  ·  心情 ${stats.mood}%  ·  精力 ${stats.energy}%  ·  星星币 ${stats.coins}  ·  陪伴 ${stats.streak} 天"
    }

    private fun removeLastTypingIndicator() {
        if (messageColumn.childCount <= 0) return
        val last = messageColumn.getChildAt(messageColumn.childCount - 1)
        if (last.tag == "typing") messageColumn.removeView(last)
    }

    private fun addMessage(name: String, text: String, fromUser: Boolean, typing: Boolean = false) {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (fromUser) Gravity.END else Gravity.START
            tag = if (typing) "typing" else null
        }
        val label = TextView(this).apply {
            this.text = name
            textSize = 11f
            setTextColor(Color.rgb(129, 135, 168))
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(if (fromUser) Color.WHITE else Color.rgb(67, 73, 107))
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = if (fromUser) {
                bubbleBackground(Color.rgb(108, 157, 255), Color.rgb(108, 157, 255))
            } else {
                bubbleBackground(Color.rgb(244, 247, 255), Color.rgb(222, 231, 255))
            }
            maxWidth = dp(260)
        }
        wrapper.addView(label)
        wrapper.addView(bubble)
        messageColumn.addView(wrapper, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        })
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun bubbleBackground(fill: Int, stroke: Int) = android.graphics.drawable.GradientDrawable().apply {
        cornerRadius = dp(16).toFloat()
        setColor(fill)
        setStroke(dp(1), stroke)
    }

    private fun chipBackground() = android.graphics.drawable.GradientDrawable().apply {
        cornerRadius = dp(18).toFloat()
        setColor(Color.rgb(244, 247, 255))
        setStroke(dp(1), Color.rgb(221, 230, 255))
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
