/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.hadoop.hdds.scm.pipelines.standalone;

import com.google.common.base.Preconditions;
import org.apache.hadoop.hdds.scm.container.common.helpers.Pipeline;
import org.apache.hadoop.hdds.scm.container.common.helpers.PipelineID;
import org.apache.hadoop.hdds.scm.container.placement.algorithms
    .ContainerPlacementPolicy;
import org.apache.hadoop.hdds.scm.node.NodeManager;
import org.apache.hadoop.hdds.scm.pipelines.Node2PipelineMap;
import org.apache.hadoop.hdds.scm.pipelines.PipelineManager;
import org.apache.hadoop.hdds.scm.pipelines.PipelineSelector;
import org.apache.hadoop.hdds.protocol.DatanodeDetails;
import org.apache.hadoop.hdds.protocol.proto.HddsProtos.NodeState;
import org.apache.hadoop.hdds.protocol.proto.HddsProtos.ReplicationFactor;
import org.apache.hadoop.hdds.protocol.proto.HddsProtos.ReplicationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Standalone Manager Impl to prove that pluggable interface
 * works with current tests.
 */
public class StandaloneManagerImpl extends PipelineManager {
  private static final Logger LOG =
      LoggerFactory.getLogger(StandaloneManagerImpl.class);
  private final NodeManager nodeManager;
  private final ContainerPlacementPolicy placementPolicy;
  private final long containerSize;
  private final Set<DatanodeDetails> standAloneMembers;

  /**
   * Constructor for Standalone Node Manager Impl.
   * @param nodeManager - Node Manager.
   * @param placementPolicy - Placement Policy
   * @param containerSize - Container Size.
   */
  public StandaloneManagerImpl(NodeManager nodeManager,
      ContainerPlacementPolicy placementPolicy, long containerSize,
      Node2PipelineMap map) {
    super(map);
    this.nodeManager = nodeManager;
    this.placementPolicy = placementPolicy;
    this.containerSize =  containerSize;
    this.standAloneMembers = new HashSet<>();
  }


  /**
   * Allocates a new standalone Pipeline from the free nodes.
   *
   * @param factor - One
   * @return Pipeline.
   */
  public Pipeline allocatePipeline(ReplicationFactor factor) {
    List<DatanodeDetails> newNodesList = new LinkedList<>();
    List<DatanodeDetails> datanodes = nodeManager.getNodes(NodeState.HEALTHY);
    int count = getReplicationCount(factor);
    for (DatanodeDetails datanode : datanodes) {
      Preconditions.checkNotNull(datanode);
      if (!standAloneMembers.contains(datanode)) {
        newNodesList.add(datanode);
        if (newNodesList.size() == count) {
          // once a datanode has been added to a pipeline, exclude it from
          // further allocations
          standAloneMembers.addAll(newNodesList);
          PipelineID pipelineID = PipelineID.randomId();
          LOG.info("Allocating a new standalone pipeline of size: {} id: {}",
              count, pipelineID);
          return PipelineSelector.newPipelineFromNodes(newNodesList,
              ReplicationType.STAND_ALONE, ReplicationFactor.ONE, pipelineID);
        }
      }
    }
    return null;
  }

  public void initializePipeline(Pipeline pipeline) {
    // Nothing to be done for standalone pipeline
  }

  /**
   * Close the pipeline.
   */
  public void closePipeline(Pipeline pipeline) {
    super.closePipeline(pipeline);
    for (DatanodeDetails node : pipeline.getMachines()) {
      // A node should always be the in standalone members list.
      Preconditions.checkArgument(standAloneMembers.remove(node));
    }
  }

  /**
   * list members in the pipeline .
   *
   * @param pipelineID
   * @return the datanode
   */
  @Override
  public List<DatanodeDetails> getMembers(PipelineID pipelineID)
      throws IOException {
    return null;
  }

  /**
   * Update the datanode list of the pipeline.
   *
   * @param pipelineID
   * @param newDatanodes
   */
  @Override
  public void updatePipeline(PipelineID pipelineID, List<DatanodeDetails>
      newDatanodes) throws IOException {

  }
}
