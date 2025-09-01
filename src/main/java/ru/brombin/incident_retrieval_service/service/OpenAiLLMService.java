package ru.brombin.incident_retrieval_service.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import ru.brombin.incident_retrieval_service.entity.PreprocessedQuestion;
import ru.brombin.incident_retrieval_service.service.transformer.RosbertaEmbeddingModel;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OpenAiLLMService {
    ChatClient chatClient;

    private static final String PREPROCESSED_SYSTEM_PROMPT =
            "Ты специалист по семантической оптимизации пользовательских вопросов для поиска по эмбеддингам. "
                    + "Преобразуй входной ВОПРОС по правилам:\n"
                    + "1) Нормализация:\n"
                    + "   - Удали HTML, CSS, JS и спецсимволы (оставь только !?.,)\n"
                    + "   - Убери лишние пробелы и переносы строк\n"
                    + "   - Приведи все кавычки к виду \\\"\\\"\n"
                    + "   - Расшифруй сокращения: «н-р» → «например», «т.д.» → «и так далее»\n"
                    + "2) Семантическое уплотнение:\n"
                    + "   - Сохрани ключевые термины, числа, имена собственные без изменений\n"
                    + "   - Удали стоп-слова («очень», «просто», «ну») и вводные фразы («кстати», «в общем»)\n"
                    + "   - Устрани повторы, сделай формулировку точной и ёмкой\n"
                    + "   - Заменяй местоимения на конкретные референсы (напр. «он» → «алгоритм авторизации»)\n"
                    + "3) Контекстуализация:\n"
                    + "   - Добавь недостающие уточнения в [квадратных скобках], если это повышает однозначность\n"
                    + "   - Делай вопрос самодостаточным: «Как он работает?» → «Как работает алгоритм авторизации?»\n"
                    + "Формат вывода:\n"
                    + "   - Сначала выведи ТОЛЬКО итоговый очищенный и уточнённый вопрос (без комментариев)\n"
                    + "   - Если вопрос состоит из нескольких смысловых частей, раздели их пустой строкой\n"
                    + "   - Затем выведи 3 альтернативные формулировки, сохраняя смысл:\n"
                    + "Вариант 1: ...\n"
                    + "Вариант 2: ...\n"
                    + "Вариант 3: ...";

    String AG_SYSTEM_PROMPT = """
            Ты эксперт по написанию лаконичных и понятных ответов для пользователей.
            На вход получаешь:
            1) Вопрос пользователя.
            2) Контекст, состоящий из релевантных документов.
            
            Задача:
            - Сформулировать ответ на вопрос, опираясь только на предоставленный контекст.
            - Если информации недостаточно — честно сообщи об этом.
            - Ответ должен быть ясным, структурированным и по возможности кратким.
            - Не добавляй лишние комментарии.
            """;

    public PreprocessedQuestion getPreprocessedRequest(String question) {
        SystemMessage systemMessage = new SystemMessage(PREPROCESSED_SYSTEM_PROMPT);
        UserMessage userMessage = new UserMessage(question);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        String raw = chatClient.prompt(prompt).call().content();
        log.info("Предобработанный результат от LLM: " + raw);

        if (raw == null || raw.isBlank()) {
            log.info("LLM не вернула нормализированный вопрос и вариантов ответа, используем исходный");
            return new PreprocessedQuestion(question, List.of(question));
        }

        String[] lines = raw.strip().split("\\r?\\n");
        List<String> variants = new ArrayList<>();
        StringBuilder normalized = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Вариант 1:") ||
                    trimmed.startsWith("Вариант 2:") ||
                    trimmed.startsWith("Вариант 3:")) {
                // Вырезаем только текст после двоеточия
                variants.add(trimmed.substring(trimmed.indexOf(':') + 1).trim());
            } else if (!trimmed.isEmpty()) {
                normalized.append(trimmed).append("\n");
            }
        }

        return new PreprocessedQuestion(normalized.toString().trim(), variants);
    }

    public String generateAnswerFromContext(String userQuestion, String context) {
        SystemMessage systemMessage = new SystemMessage(AG_SYSTEM_PROMPT);
        String userContent = "Вопрос пользователя: " + userQuestion + "\n\nКонтекст документов:\n" + context;

        UserMessage userMessage = new UserMessage(userContent);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        String response = chatClient.prompt(prompt).call().content();

        return response != null ? response.strip() : "Не удалось получить ответ";
    }
}
