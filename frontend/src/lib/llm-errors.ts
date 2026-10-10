/** User-facing explanations for stable run error codes (see LlmErrorCode.java). */
export function failureMessageFor(errorCode: string | null | undefined): string {
  switch (errorCode) {
    case 'LLM_NOT_CONFIGURED':
      return 'The AI provider is not configured. Check the provider, model, and API key in your .env.'
    case 'LLM_UNAUTHORIZED':
      return 'The AI provider rejected the API key. Check the key in your .env and restart the backend.'
    case 'MODEL_NOT_FOUND':
      return 'The configured model was not found at the provider. Check the *_MODEL values in your .env.'
    case 'LLM_RATE_LIMITED':
      return 'The AI provider is rate limiting requests. Wait a minute and try again.'
    case 'LLM_TIMEOUT':
      return 'The AI provider took too long to respond. Try again or pick a faster model.'
    case 'LLM_CONNECTION_FAILED':
      return 'Could not reach the AI provider. Check your network and the provider URL (Ollama must be running in dev).'
    case 'LLM_INVALID_REQUEST':
      return 'The AI provider rejected the request. Check that the model name matches the provider.'
    case 'LLM_INVALID_OUTPUT':
      return 'The model returned output that could not be validated.'
    case 'LLM_FAILED':
      return 'The AI provider could not complete the request.'
    case 'CANARY_LEAK':
      return 'The model returned an unsafe response, so it was discarded.'
    case 'APP_RESTART':
      return 'The server restarted mid-run.'
    default:
      return 'Something went wrong.'
  }
}
