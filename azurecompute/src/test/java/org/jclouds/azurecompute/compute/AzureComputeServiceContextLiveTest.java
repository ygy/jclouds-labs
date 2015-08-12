/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.azurecompute.compute;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

import org.jclouds.azurecompute.compute.options.AzureComputeTemplateOptions;
import org.jclouds.azurecompute.internal.BaseAzureComputeApiLiveTest;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.internal.BaseComputeServiceContextLiveTest;
import org.jclouds.ssh.SshClient;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.testng.annotations.Test;

import com.google.common.collect.Iterables;
import com.google.inject.Module;

@Test(groups = "live", testName = "AzureComputeServiceContextLiveTest")
public class AzureComputeServiceContextLiveTest extends BaseComputeServiceContextLiveTest {

   @Override
   protected Module getSshModule() {
      return new SshjSshClientModule();
   }

   public AzureComputeServiceContextLiveTest() {
      provider = "azurecompute";
   }

   /**
    * @throws RunNodesException
    */
   @Test
   public void testLaunchNode() throws RunNodesException {
      final int rand = new Random().nextInt(999);
      final String groupName = String.format("%s%d-group-acsclt", System.getProperty("user.name"), rand);

      final String name = String.format("%1.5s%dacsclt", System.getProperty("user.name"), rand);

      final TemplateBuilder templateBuilder = view.getComputeService().templateBuilder();
      templateBuilder.imageId(BaseAzureComputeApiLiveTest.IMAGE_NAME);
      templateBuilder.hardwareId("BASIC_A0");
      templateBuilder.locationId(BaseAzureComputeApiLiveTest.LOCATION);
      final Template tmp = templateBuilder.build();

      // test passing custom options
      final AzureComputeTemplateOptions options = tmp.getOptions().as(AzureComputeTemplateOptions.class);
      options.inboundPorts(22);
      options.nodeNames(Arrays.asList(name));

      NodeMetadata node = null;
      try {
         final Set<? extends NodeMetadata> nodes = view.getComputeService().createNodesInGroup(groupName, 1, tmp);
         node = Iterables.getOnlyElement(nodes);
         final SshClient client = view.utils().sshForNode().apply(node);
         client.connect();
         final ExecResponse hello = client.exec("echo hello");
         assertThat(hello.getOutput().trim()).isEqualTo("hello");
      } finally {
         if (node != null) {
            view.getComputeService().destroyNode(node.getId());
         }
      }
   }

   @Test
   public void testLaunchNodeAndNetwork() throws RunNodesException {
      final int rand = new Random().nextInt(999);
      final String groupName = String.format("%s%d-group-acsclt", System.getProperty("user.name"), rand);

      final String name = String.format("%1.5s%dacsclt", System.getProperty("user.name"), rand);

      final TemplateBuilder templateBuilder = view.getComputeService().templateBuilder();
      templateBuilder.imageId(BaseAzureComputeApiLiveTest.IMAGE_NAME);
      templateBuilder.hardwareId("BASIC_A0");
      templateBuilder.locationId(BaseAzureComputeApiLiveTest.LOCATION);
      final Template tmp = templateBuilder.build();

      // test passing custom options
      final AzureComputeTemplateOptions options = tmp.getOptions().as(AzureComputeTemplateOptions.class);
      options.inboundPorts(22);
      // NB the user must know prior virtualNetworkName and subnetNames
      options.virtualNetworkName(BaseAzureComputeApiLiveTest.VIRTUAL_NETWORK_NAME);
      options.subnetNames("jclouds-1");
      options.nodeNames(Arrays.asList(name));

      NodeMetadata node = null;
      try {
         final Set<? extends NodeMetadata> nodes = view.getComputeService().createNodesInGroup(groupName, 1, tmp);
         node = Iterables.getOnlyElement(nodes);
         final SshClient client = view.utils().sshForNode().apply(node);
         client.connect();
         final ExecResponse hello = client.exec("echo hello");
         assertThat(hello.getOutput().trim()).isEqualTo("hello");
      } finally {
         if (node != null) {
            view.getComputeService().destroyNode(node.getId());
         }
      }
   }

   @Test(expectedExceptions = { IllegalStateException.class })
       public void testNotExistingStorageAccount() throws RunNodesException {
      final int rand = new Random().nextInt(999);
      final String groupName = String.format("%s%d-group-acsclt", System.getProperty("user.name"), rand);

      final String storageServiceName = "not3x1st1ng";

      final Template template = view.getComputeService().templateBuilder().build();

      // test passing custom options
      final AzureComputeTemplateOptions options = template.getOptions().as(AzureComputeTemplateOptions.class);
      options.storageAccountName(storageServiceName);

      Set<? extends NodeMetadata> nodes = view.getComputeService().createNodesInGroup(groupName, 1, template);
   }

}
