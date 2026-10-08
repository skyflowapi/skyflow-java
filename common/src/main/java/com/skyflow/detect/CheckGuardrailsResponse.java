package com.skyflow.detect;

/**
 * Response of {@code checkGuardrails}.
 */
public final class CheckGuardrailsResponse {
    private final String text;
    private final GuardrailsValidation validation;
    private final Boolean toxic;
    private final Boolean deniedTopic;

    public CheckGuardrailsResponse(String text, GuardrailsValidation validation, Boolean toxic, Boolean deniedTopic) {
        this.text = text;
        this.validation = validation;
        this.toxic = toxic;
        this.deniedTopic = deniedTopic;
    }

    /**
     * @return The text that was screened.
     */
    public String getText() {
        return text;
    }

    /**
     * @return PASSED when no guardrail was triggered, FAILED otherwise.
     */
    public GuardrailsValidation getValidation() {
        return validation;
    }

    /**
     * @return Whether the text was judged toxic. Null when toxicity was not checked.
     */
    public Boolean getToxic() {
        return toxic;
    }

    /**
     * @return Whether the text touched a denied topic. Null when no topics were supplied.
     */
    public Boolean getDeniedTopic() {
        return deniedTopic;
    }

    @Override
    public String toString() {
        return "CheckGuardrailsResponse{" +
                "text=" + text +
                ", validation=" + validation +
                ", toxic=" + toxic +
                ", deniedTopic=" + deniedTopic +
                '}';
    }
}
