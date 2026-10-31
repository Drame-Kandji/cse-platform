package sn.cse.chatbot.application.service;

/**
 * Texte produit par l'étape « genererReponse ».
 *
 * @param texte réponse markdown
 * @param modeDegrade vrai si produite sans LLM
 */
public record ReponseGeneree(String texte, boolean modeDegrade) {}
