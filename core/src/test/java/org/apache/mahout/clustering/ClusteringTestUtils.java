/**
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

package org.apache.mahout.clustering;

import com.google.common.io.Closeables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.mahout.clustering.lda.LDASampler;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.SparseRowMatrix;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.math.function.DoubleFunction;

import java.io.IOException;
import java.util.Random;

public final class ClusteringTestUtils {

  private ClusteringTestUtils() {
  }

  public static void writePointsToFile(Iterable<VectorWritable> points,
                                       Path path,
                                       FileSystem fs,
                                       Configuration conf) throws IOException {
    writePointsToFile(points, false, path, fs, conf);
  }

  public static void writePointsToFile(Iterable<VectorWritable> points,
                                       boolean intWritable,
                                       Path path,
                                       FileSystem fs,
                                       Configuration conf) throws IOException {
    SequenceFile.Writer writer = new SequenceFile.Writer(fs,
                                                         conf,
                                                         path,
                                                         intWritable ? IntWritable.class : LongWritable.class,
                                                         VectorWritable.class);
    try {
      int recNum = 0;
      for (VectorWritable point : points) {
        writer.append(intWritable ? new IntWritable(recNum++) : new LongWritable(recNum++), point);
      }
    } finally {
      Closeables.closeQuietly(writer);
    }
  }

  public static Matrix sampledCorpus(Matrix matrix, Random random,
      int numDocs, int numSamples, int numTopicsPerDoc) {
    Matrix corpus = new SparseRowMatrix(numDocs, matrix.numCols());
    LDASampler modelSampler = new LDASampler(matrix, random);
    Vector topicVector = new DenseVector(matrix.numRows());
    for(int i = 0; i < numTopicsPerDoc; i++) {
      int topic = random.nextInt(topicVector.size());
      topicVector.set(topic, topicVector.get(topic) + 1);
    }
    for(int docId = 0; docId < numDocs; docId++) {
      for(int sample : modelSampler.sample(topicVector, numSamples)) {
        corpus.set(docId, sample, corpus.get(docId, sample) + 1);
      }
    }
    return corpus;
  }

  public static Matrix randomStructuredModel(int numTopics, int numTerms) {
    return randomStructuredModel(numTopics, numTerms, new DoubleFunction() {
      @Override public double apply(double d) {
        return 1.0 / (1 + Math.abs(d));
      }
    });
  }

  public static Matrix randomStructuredModel(int numTopics, int numTerms, DoubleFunction decay) {
    Matrix model = new DenseMatrix(numTopics, numTerms);
    int width = numTerms / numTopics;
    for(int topic = 0; topic < numTopics; topic++) {
      int topicCentroid = width * (1+topic);
      for(int i = 0; i < numTerms; i++) {
        int distance = Math.abs(topicCentroid - i);
        if(distance > numTerms / 2) {
          distance = numTerms - distance;
        }
        double v = decay.apply(distance);
        model.set(topic, i, v);
      }
    }
    return model;
  }
}
