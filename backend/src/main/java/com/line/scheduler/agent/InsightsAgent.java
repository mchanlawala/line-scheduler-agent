package com.line.scheduler.agent;

import com.line.scheduler.domain.ScheduleResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class InsightsAgent {

    private final ChatLanguageModel llm;
    private final boolean aiEnabled;

    public InsightsAgent(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.model:llama3.2}") String modelName,
            @Value("${ollama.temperature:0.2}") double temperature) {
        ChatLanguageModel tmpLlm = null;
        boolean tmpEnabled = false;
        try {
            tmpLlm = OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .temperature(temperature)
                    .build();
            tmpEnabled = true;
            log.info("AI agent enabled for schedule insights");
        } catch (Exception e) {
            log.warn("Failed to initialize Ollama: {}", e.getMessage());
        }
        this.llm = tmpLlm;
        this.aiEnabled = tmpEnabled;
    }

    public String analyseSchedule(ScheduleResult schedule) {
        if (!aiEnabled || llm == null) {
            return buildRuleBasedInsights(schedule);
        }
        try {
            String summary = buildScheduleSummary(schedule);
            String prompt = "You are a production scheduler. Analyse the following schedule and give concise, actionable insights:\n\n" + summary;
            Response<AiMessage> response = llm.generate(List.of(
                    SystemMessage.from("You are an expert in solar panel production scheduling."),
                    UserMessage.from(prompt)
            ));
            return response.content().text();
        } catch (Exception e) {
            log.error("AI analysis failed", e);
            return buildRuleBasedInsights(schedule);
        }
    }

    private String buildScheduleSummary(ScheduleResult schedule) {
        return "Total orders: " + schedule.getSummary().getTotalOrders() +
               "\nTotal panels: " + schedule.getSummary().getTotalPanels() +
               "\nAverage lead time: " + schedule.getSummary().getAvgLeadTimeHours() + " hours" +
               "\nLines utilized: " + schedule.getSummary().getLinesUtilized();
    }

    private String buildRuleBasedInsights(ScheduleResult schedule) {
        return "Rule‑based insights: " + schedule.getSummary().getTotalOrders() + " orders, " +
               schedule.getSummary().getTotalPanels() + " panels. Average lead time: " +
               schedule.getSummary().getAvgLeadTimeHours() + " hours. " +
               "Review any delayed orders and balance line utilisation.";
    }
}