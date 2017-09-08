package org.ihtsdo.elasticsnomed.core.data.services.classification;

import org.ihtsdo.elasticsnomed.core.data.services.classification.pojo.ClassificationStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Collections;

@Service
class RemoteClassificationServiceClient {

	private String serviceUrl;
	private RestTemplate restTemplate;

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final HttpHeaders MULTIPART_HEADERS = new HttpHeaders();
	static {
		MULTIPART_HEADERS.setContentType(MediaType.MULTIPART_FORM_DATA);
	}

	public RemoteClassificationServiceClient(@Value("${classification-service.url}") String serviceUrl) {
		this.serviceUrl = serviceUrl;
		restTemplate = new RestTemplateBuilder().rootUri(serviceUrl).build();
	}

	/**
	 *
	 * @param previousPackage The path or identifier of the previous RF2 snapshot archive.
	 * @param deltaFile The RF2 delta archive of the content to be added to the previous package and classified.
	 * @param branchPath The path of the branch which is being classified.
	 * @param reasonerId The identifier of the reasoner to use for classification.
	 * @return remoteClassificationId The identifier of the classification run on the remote service.
	 * @throws RestClientException if something goes wrong when communicating with the remote service.
	 */
	String createClassification(String previousPackage, File deltaFile, String branchPath, String reasonerId) throws RestClientException {
		MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
		params.put("previousRelease", Collections.singletonList(previousPackage));
		params.put("rf2Delta", Collections.singletonList(new FileSystemResource(deltaFile)));
		params.put("branch", Collections.singletonList(branchPath));
		params.put("reasonerId", Collections.singletonList(reasonerId));


		ResponseEntity<Void> response = restTemplate.postForEntity("/classifications", new HttpEntity<>(params, MULTIPART_HEADERS), Void.class);
		String location = response.getHeaders().getLocation().toString();
		String remoteClassificationId = location.substring(location.lastIndexOf("/") + 1);

		logger.info("Created classification id:{}, location:{}", remoteClassificationId, location);



		return remoteClassificationId;
	}

	ClassificationStatusResponse getStatus(String classificationId) {
		return restTemplate.getForObject("/classifications/{classificationId}", ClassificationStatusResponse.class, classificationId);
	}
}