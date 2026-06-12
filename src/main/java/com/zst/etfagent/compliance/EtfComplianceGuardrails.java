package com.zst.etfagent.compliance;

import com.agent4j.api.InputGuardrail;
import com.agent4j.api.OutputGuardrail;
import com.agent4j.model.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class EtfComplianceGuardrails {

    private static final Pattern BLOCKED_PROMISES = Pattern.compile("(稳赚|保本|保证收益|必涨|一定涨|明天.*涨|买入|卖出|满仓)");
    private static final String DISCLAIMER = "\n\n合规提示：以上内容仅用于 B2B 研究辅助和产品比较，不构成投资建议、收益承诺或交易指令；基金选择仍需结合客户适当性、风险承受能力和最新产品文件。";

    public InputGuardrail inputGuardrail() {
        return (messages, context) -> {
            List<Message> rewritten = messages.stream()
                    .map(message -> message.getMessageType() == Message.MessageType.USER
                            ? Message.user(message.getText() + "\n\n请按研究参考口径回答，不要输出保证收益、明确买卖指令或短期涨跌预测。")
                            : message)
                    .toList();
            return InputGuardrail.InputGuardrailResult.pass(rewritten);
        };
    }

    public OutputGuardrail outputGuardrail() {
        return (output, context) -> {
            String text = output == null ? "" : output.toString();
            if (BLOCKED_PROMISES.matcher(text).find()) {
                return OutputGuardrail.OutputGuardrailResult.pass(
                        "该问题或回答涉及收益承诺、明确交易指令或短期涨跌预测。请改为从板块、指数、ETF 产品指标、风险因素和适当性角度进行研究比较。" + DISCLAIMER
                );
            }
            if (!text.contains("不构成投资建议") && !text.contains("合规提示")) {
                text = text + DISCLAIMER;
            }
            return OutputGuardrail.OutputGuardrailResult.pass(text);
        };
    }
}
