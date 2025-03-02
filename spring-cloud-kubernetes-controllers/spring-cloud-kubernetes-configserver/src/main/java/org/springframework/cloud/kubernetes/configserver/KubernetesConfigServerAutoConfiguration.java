/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.cloud.kubernetes.configserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.kubernetes.client.openapi.apis.CoreV1Api;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.config.ConfigServerAutoConfiguration;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.kubernetes.client.KubernetesClientAutoConfiguration;
import org.springframework.cloud.kubernetes.client.config.KubernetesClientConfigMapPropertySource;
import org.springframework.cloud.kubernetes.client.config.KubernetesClientSecretsPropertySource;
import org.springframework.cloud.kubernetes.commons.ConditionalOnKubernetesConfigEnabled;
import org.springframework.cloud.kubernetes.commons.ConditionalOnKubernetesEnabled;
import org.springframework.cloud.kubernetes.commons.ConditionalOnKubernetesSecretsEnabled;
import org.springframework.cloud.kubernetes.commons.KubernetesNamespaceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.MapPropertySource;

import static org.springframework.cloud.kubernetes.configserver.KubernetesPropertySourceSupplier.namespaceSplitter;

/**
 * @author Ryan Baxter
 */
@Configuration
@AutoConfigureAfter({ KubernetesClientAutoConfiguration.class })
@AutoConfigureBefore({ ConfigServerAutoConfiguration.class })
@ConditionalOnKubernetesEnabled
@EnableConfigurationProperties(KubernetesConfigServerProperties.class)
public class KubernetesConfigServerAutoConfiguration {

	@Bean
	@Profile("kubernetes")
	public EnvironmentRepository kubernetesEnvironmentRepository(CoreV1Api coreV1Api,
			List<KubernetesPropertySourceSupplier> kubernetesPropertySourceSuppliers,
			KubernetesNamespaceProvider kubernetesNamespaceProvider) {
		return new KubernetesEnvironmentRepository(coreV1Api, kubernetesPropertySourceSuppliers,
				kubernetesNamespaceProvider.getNamespace());
	}

	@Bean
	@ConditionalOnKubernetesConfigEnabled
	@ConditionalOnProperty(value = "spring.cloud.kubernetes.config.enableApi", matchIfMissing = true)
	public KubernetesPropertySourceSupplier configMapPropertySourceSupplier(
			KubernetesConfigServerProperties properties) {
		return (coreApi, applicationName, namespace, springEnv) -> {
			List<String> namespaces = namespaceSplitter(properties.getSecretsNamespaces(), namespace);
			List<MapPropertySource> propertySources = new ArrayList<>();
			namespaces.forEach(space -> propertySources
					.add(new KubernetesClientConfigMapPropertySource(coreApi, applicationName, space, springEnv, "")));
			return propertySources;
		};
	}

	@Bean
	@ConditionalOnKubernetesSecretsEnabled
	@ConditionalOnProperty("spring.cloud.kubernetes.secrets.enableApi")
	public KubernetesPropertySourceSupplier secretsPropertySourceSupplier(KubernetesConfigServerProperties properties) {
		return (coreApi, applicationName, namespace, springEnv) -> {
			List<String> namespaces = namespaceSplitter(properties.getSecretsNamespaces(), namespace);
			List<MapPropertySource> propertySources = new ArrayList<>();
			namespaces.forEach(space -> propertySources.add(new KubernetesClientSecretsPropertySource(coreApi,
					applicationName, space, springEnv, new HashMap<>())));
			return propertySources;
		};
	}

}
