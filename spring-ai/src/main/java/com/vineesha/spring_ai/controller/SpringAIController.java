package com.vineesha.spring_ai.controller;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class SpringAIController {

	private final ChatClient chatClient;

	SpringAIController(ChatClient.Builder chatClientBuilder){
        this.chatClient= chatClientBuilder.build();
    }

	@Autowired
	private OpenAiImageModel openAiImageModel;

	@Autowired
	private ChatModel chatModel;

	@Autowired
	private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

	@Autowired
	private OpenAiAudioSpeechModel openAiAudioSpeechModel;

//	here in this method if we pass any text as prompt or ask any question, then it gives us the text answer
//	Ex: /prompt?message="Tell me a joke"
	@GetMapping("/prompt")
	public String prompt(@RequestParam String message) {
//		return chatClient.prompt(message).toString();
		return chatClient.prompt().user(message).call().content();
	}

//	This will return popular sports persions and achievements if we pass sports name
//	Ex: /?sports=Cricket
	@GetMapping("/")
	public String findPopularSportsPerson(@RequestParam String sports) {
		String message = "List of 5 most popular personalities in {sports} along with their Career Achievements. Show the details in proper Readable Format.";

//		this is to set the barrier to the prompt about sharing information to the user 
//		SystemMessage systemMessage = new SystemMessage("Your primary function is to share the information about the sports personalities. If someone ask about anything else, you can say you only share about sports personalities");
//		UserMessage userMessage = new UserMessage(String.format("List of 5 most popular personalities in %s along with their Career Achievements. Show the details in proper Readable Format.", sports));
//		Prompt prompt2 = new Prompt(userMessage);
//		Prompt prompt2 = new Prompt(List.of(systemMessage, userMessage));
//		return chatClient.prompt(prompt2).toString();

//		this is for universal information like in sports, film making, hollywood etc.
		PromptTemplate promptTemplate = new PromptTemplate(message);
		Prompt prompt = promptTemplate.create(Map.of("sports", sports));
		ChatResponse response = chatClient
                .prompt(prompt)
                .call()
                .chatResponse();

//		return chatClient.prompt(prompt).call().content();
		return response.getResult().getOutput().getContent();
	}

//	this will generate image with specified qualities for the prompt input
//	Ex: /image/"An Elephant playing with Basketball"
//	Ex: /image/"Create a logo for my personal youtube channel"
	@GetMapping("/image/{prompt}")
	public String generateImage(@PathVariable("prompt") String prompt) {
		ImageResponse imageResponse = openAiImageModel.call(new ImagePrompt(prompt,
				OpenAiImageOptions.builder().withHeight(1024).withQuality(prompt).withWidth(1024).withN(1).build()));
		return imageResponse.getResult().getOutput().getUrl();
	}

//	this will take image as input and explains about the image
//	Ex: /image-to-text
	@GetMapping("/image-to-text")
	public String generateImageToText() {
		String response = ChatClient.create(chatModel).prompt()
				.user(userSpec -> userSpec.text("Explain what do you see in this Image")
						.media(MimeTypeUtils.IMAGE_JPEG, new FileSystemResource("/spring-ai/src/main/resources/computer.jpg")))
				.call().content();
		return response;
	}
	
//	this will take audio as input and gives transcripts of the audio
//	Ex: /audio-to-text
	@GetMapping("/audio-to-text")
    public String generateTranscription() {

        OpenAiAudioTranscriptionOptions options
                = OpenAiAudioTranscriptionOptions.builder()
                .withLanguage("es")
                .withResponseFormat(OpenAiAudioApi.TranscriptResponseFormat.SRT)
                .withTemperature(0f)
                .build();

        AudioTranscriptionPrompt prompt
                = new AudioTranscriptionPrompt(
                        new FileSystemResource("/Users/shabbir/Documents/java-workspace/spring-ai-multimodel/src/main/resources/harvard.wav"),
                options);

        AudioTranscriptionResponse response
                = openAiAudioTranscriptionModel.call(prompt);

        return response.getResult().getOutput();
    }


//	this will take text as input and output the audio download file
//	Ex: /text-to-audio/"Let me explain these java program to you"
//	for better long descriptive text pass prompt as json object in @RequestBody
    @GetMapping("/text-to-audio/{prompt}")
    public ResponseEntity<Resource> generateAudio(@PathVariable("prompt") String prompt) {
        OpenAiAudioSpeechOptions options
                = OpenAiAudioSpeechOptions.builder()
                .withModel("tts-1")
                .withSpeed(1.0f)
                .withVoice(OpenAiAudioApi.SpeechRequest.Voice.NOVA)
                .build();

        SpeechPrompt speechPrompt
                = new SpeechPrompt(prompt,options);

        SpeechResponse response
                    = openAiAudioSpeechModel.call(speechPrompt);

        byte[] responseBytes = response.getResult().getOutput();

        ByteArrayResource byteArrayResource
                = new ByteArrayResource(responseBytes);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(byteArrayResource.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("whatever.mp3")
                                .build().toString())
                .body(byteArrayResource);
    }
    
//    this is to get the meaning for the word in english
//    Ex: /getMeaning?word=da;fj
    @PostMapping(path = "/getMeaning")
    public ResponseEntity<String> getMeaning(@RequestParam("word") String word) {
        String apiUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/" + word;
        RestTemplate restTemplate = new RestTemplate();
        
        try {
            String response = restTemplate.getForObject(apiUrl, String.class);
            JSONArray jsonArray = new JSONArray(response);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            JSONArray meaningsArray = jsonObject.getJSONArray("meanings");
            JSONObject firstMeaning = meaningsArray.getJSONObject(0);
            JSONArray definitionsArray = firstMeaning.getJSONArray("definitions");
            String meaning = definitionsArray.getJSONObject(0).getString("definition");

            return ResponseEntity.ok(meaning);
        } catch (Exception e) {
            return ResponseEntity.ok("No meaning found for this word or error retrieving the meaning.");
        }
    }
}
