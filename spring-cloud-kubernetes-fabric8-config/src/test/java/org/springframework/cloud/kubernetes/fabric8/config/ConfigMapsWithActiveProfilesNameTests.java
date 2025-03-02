/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.kubernetes.fabric8.config;

import java.util.HashMap;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.kubernetes.fabric8.config.example.App;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.kubernetes.fabric8.config.ConfigMapTestUtil.readResourceFile;

/**
 * @author Ali Shahbour
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = App.class,
		properties = { "spring.application.name=configmap-with-active-profile-name-example",
				"spring.cloud.kubernetes.reload.enabled=false" })
@ActiveProfiles("development")
@AutoConfigureWebTestClient
@EnableKubernetesMockClient(crud = true, https = false)
public class ConfigMapsWithActiveProfilesNameTests {

	private static final String APPLICATION_NAME = "configmap-with-active-profile-name-example";

	private static KubernetesClient mockClient;

	@Autowired(required = false)
	Config config;

	@Autowired
	private WebTestClient webClient;

	@BeforeAll
	public static void setUpBeforeClass() {

		// Configure the kubernetes master url to point to the mock server
		System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, mockClient.getConfiguration().getMasterUrl());
		System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
		System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
		System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false");
		System.setProperty(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "test");
		System.setProperty(Config.KUBERNETES_HTTP2_DISABLE, "true");

		HashMap<String, String> data = new HashMap<>();
		data.put("application.yml", readResourceFile("application-with-profiles.yaml"));
		mockClient.configMaps().inNamespace("test").create(new ConfigMapBuilder().withNewMetadata()
				.withName(APPLICATION_NAME).endMetadata().addToData(data).build());

		HashMap<String, String> dataWithName = new HashMap<>();
		dataWithName.put("application.yml", readResourceFile("application-with-active-profiles-name.yaml"));
		mockClient.configMaps().inNamespace("test").create(new ConfigMapBuilder().withNewMetadata()
				.withName(APPLICATION_NAME + "-development").endMetadata().addToData(dataWithName).build());
	}

	@Test
	public void testGreetingEndpoint() {
		this.webClient.get().uri("/api/greeting").exchange().expectStatus().isOk().expectBody().jsonPath("content")
				.isEqualTo("Hello ConfigMap Active Profile Name, World!");
	}

	@Test
	public void testFarewellEndpoint() {
		this.webClient.get().uri("/api/farewell").exchange().expectStatus().isOk().expectBody().jsonPath("content")
				.isEqualTo("Goodbye ConfigMap default, World!");
	}

}
