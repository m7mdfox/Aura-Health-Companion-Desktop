package com.example.auradesktop.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

public class GeminiService {

    private static final String LUNG_KEY = "AIzaSyDMM218rOxBvNZKcb9C4JmS63ey3EAqtM0";
    private static final String BONE_KEY = "AIzaSyDMM218rOxBvNZKcb9C4JmS63ey3EAqtM0";
    private static final String BRAIN_KEY = "AIzaSyAOjLOYwpYH4sGZ_IIheFaD7Cf8-wv86F8";
    private static final String DENTAL_KEY = "AIzaSyBzs0czURZ77oAViskuNNtUULVyDRZIXxI";
    private static final String EYE_KEY = "AIzaSyDuVnoOLyTVmrVHgVQwOAONCHWCt4FOuCc";
    
    // Constant Prompts
    private static final String LUNG_PROMPT = "You are an expert radiologist. Analyze this CHEST X-RAY for lung diseases. " +
            "Respond ONLY with a valid JSON object in the following format: " +
            "{ \"classification\": \"Normal\" or \"Abnormal\", " +
            "\"verdict\": \"Short Title (e.g. Injured with Pneumonia)\", " +
            "\"details\": \"GENERATE A VERY DETAILED, MULTI-SECTION PROFESSIONAL MEDICAL REPORT.\\n" +
            "Use '\\\\n' (newline characters) to separate all sections. Format exactly like this:\\n" +
            "1. CLINICAL IDICATION: [Details]\\n" +
            "2. TECHNIQUE: [Details]\\n" +
            "3. FINDINGS:\\n[Detailed bullet points]\\n" +
            "4. IMPRESSION:\\n[Conclusion]\\n" +
            "5. RECOMMENDATIONS:\\n[Next steps]\" } " +
            "Do not use Markdown.";
            
    private static final String BONE_PROMPT = "You are an expert radiologist. Analyze this X-RAY for BONE FRACTURES or abnormalities. " +
            "Respond ONLY with a valid JSON object in the following format: " +
            "{ \"classification\": \"Normal\" or \"Abnormal\", " +
            "\"verdict\": \"Short Title (e.g. Distal Radius Fracture)\", " +
            "\"details\": \"GENERATE A VERY DETAILED, MULTI-SECTION PROFESSIONAL MEDICAL REPORT.\\n" +
            "Use '\\\\n' (newline characters) to separate all sections. Format exactly like this:\\n" +
            "1. CLINICAL InDICATION: [Details]\\n" +
            "2. TECHNIQUE: [Details]\\n" +
            "3. FINDINGS:\\n[Analyze cortical continuity, alignment, joint spaces]\\n" +
            "4. IMPRESSION:\\n[Conclusion]\\n" +
            "5. RECOMMENDATIONS:\\n[Next steps]\" } " +
            "Do not use Markdown.";

    private static final String BRAIN_PROMPT = "You are an expert radiologist. Analyze this MRI or CT SCAN for BRAIN issues (Stroke, Tumor, Bleeding/Clot). " +
            "Respond ONLY with a valid JSON object in the following format: " +
            "{ \"classification\": \"Normal\" or \"Abnormal\", " +
            "\"verdict\": \"Short Title (e.g. Acute Ischemic Stroke)\", " +
            "\"details\": \"GENERATE A VERY DETAILED, MULTI-SECTION PROFESSIONAL MEDICAL REPORT.\\n" +
            "Use '\\\\n' (newline characters) to separate all sections. Format exactly like this:\\n" +
            "1. CLINICAL INDICATION: [Suspected Stroke/Trauma]\\n" +
            "2. TECHNIQUE: [CT/MRI Sequence]\\n" +
            "3. FINDINGS:\\n[Detailed analysis of parenchyma, ventricles, midline shift, hemorrhage]\\n" +
            "4. IMPRESSION:\\n[Conclusion]\\n" +
            "5. RECOMMENDATIONS:\\n[Next steps]\" } " +
            "Do not use Markdown.";

    private static final String DENTAL_PROMPT = "You are an expert dentist/oral surgeon. Analyze this DENTAL X-RAY (Panoramic/Periapical) for oral issues. " +
            "Respond ONLY with a valid JSON object in the following format: " +
            "{ \"classification\": \"Normal\" or \"Abnormal\", " +
            "\"verdict\": \"Short Title (e.g. Impacted Wisdom Tooth / Severe Caries)\", " +
            "\"details\": \"GENERATE A VERY DETAILED, MULTI-SECTION PROFESSIONAL MEDICAL REPORT.\\n" +
            "Use '\\\\n' (newline characters) to separate all sections. Format exactly like this:\\n" +
            "1. CLINICAL IDICATION: [Dental Checkup/Pain]\\n" +
            "2. TYPE OF IMAGE: [OPG/Bitewing/etc.]\\n" +
            "3. FINDINGS:\\n[Analyze teeth numbers, caries, root infections, bone levels, impactions]\\n" +
            "4. IMPRESSION:\\n[Conclusion]\\n" +
            "5. RECOMMENDATIONS:\\n[Treatments like Fillings, Root Canal, Extraction]\" } " +
            "Do not use Markdown.";

    private static final String EYE_PROMPT = "You are an expert ophthalmologist. Analyze this EYE IMAGE (Fundus/External/OCT) for ocular diseases. " +
            "Respond ONLY with a valid JSON object in the following format: " +
            "{ \"classification\": \"Normal\" or \"Abnormal\", " +
            "\"verdict\": \"Short Title (e.g. Diabetic Retinopathy - Moderate)\", " +
            "\"details\": \"GENERATE A VERY DETAILED, MULTI-SECTION PROFESSIONAL MEDICAL REPORT.\\n" +
            "Use '\\\\n' (newline characters) to separate all sections. Format exactly like this:\\n" +
            "1. CLINICAL INDICATION: [Eye Exam/Vision Loss]\\n" +
            "2. DISEASE TYPE: [e.g. Glaucoma, Cataract, DR]\\n" +
            "3. SEVERITY/STAGE: [e.g. Early, Moderate, Advanced]\\n" +
            "4. RISK SCORE: [Low/Medium/High] (Note: This is an AI assessment, not a final diagnosis)\\n" +
            "5. FINDINGS:\\n[Detailed analysis of optic disc, macula, vessels, lens]\\n" +
            "6. IMPRESSION:\\n[Conclusion]\\n" +
            "7. RECOMMENDATIONS:\\n[Next steps/Referral]\" } " +
            "Do not use Markdown.";

    public static JsonObject analyzeLung(File imageFile) throws IOException {
        return analyze(imageFile, LUNG_KEY, LUNG_PROMPT);
    }
    
    public static JsonObject analyzeBone(File imageFile) throws IOException {
        return analyze(imageFile, BONE_KEY, BONE_PROMPT);
    }

    public static JsonObject analyzeBrain(File imageFile) throws IOException {
        return analyze(imageFile, BRAIN_KEY, BRAIN_PROMPT);
    }

    public static JsonObject analyzeDental(File imageFile) throws IOException {
        return analyze(imageFile, DENTAL_KEY, DENTAL_PROMPT);
    }

    public static JsonObject analyzeEye(File imageFile) throws IOException {
        return analyze(imageFile, EYE_KEY, EYE_PROMPT);
    }

    private static JsonObject analyze(File imageFile, String apiKey, String promptText) throws IOException {
        String urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        
        // 1. Encode image
        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        String encodedString = Base64.getEncoder().encodeToString(fileContent);

        // 2. Build Payload
        JsonObject contentPart = new JsonObject();
        
        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mime_type", "image/jpeg");
        inlineData.addProperty("data", encodedString);
        
        JsonObject imagePart = new JsonObject();
        imagePart.add("inline_data", inlineData);

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", promptText);

        JsonArray parts = new JsonArray();
        parts.add(textPart);
        parts.add(imagePart);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contents);

        // Safety Settings
        JsonArray safetySettings = new JsonArray();
        String[] categories = {
            "HARM_CATEGORY_HARASSMENT", 
            "HARM_CATEGORY_HATE_SPEECH", 
            "HARM_CATEGORY_SEXUALLY_EXPLICIT", 
            "HARM_CATEGORY_DANGEROUS_CONTENT"
        };
        
        for (String category : categories) {
            JsonObject setting = new JsonObject();
            setting.addProperty("category", category);
            setting.addProperty("threshold", "BLOCK_NONE");
            safetySettings.add(setting);
        }
        requestBody.add("safetySettings", safetySettings);

        // 3. Send Request
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // 4. Read Response
        int status = con.getResponseCode();
        BufferedReader in;
        if (status > 299) {
            in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
        } else {
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        }
        
        StringBuilder contentInfo = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            contentInfo.append(line);
        }
        in.close();

        if (status > 299) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Error from AI: " + contentInfo.toString());
            return error;
        }

        // 5. Parse Response
        try {
            JsonObject jsonResponse = JsonParser.parseString(contentInfo.toString()).getAsJsonObject();
            String rawText = jsonResponse.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
            
            rawText = rawText.replace("```json", "").replace("```", "").trim();
            return JsonParser.parseString(rawText).getAsJsonObject();
            
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Failed to parse AI response. Raw output: " + contentInfo.toString());
            return error;
        }
    }
}
