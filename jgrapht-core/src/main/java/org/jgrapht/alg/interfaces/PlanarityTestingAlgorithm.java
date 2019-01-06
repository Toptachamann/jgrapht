package org.jgrapht.alg.interfaces;

import org.jgrapht.Graph;

import java.util.List;
import java.util.Map;

public interface PlanarityTestingAlgorithm<V, E> {

    boolean isPlanar();

    Embedding<V, E> getEmbedding();

    Graph<V, E> getKuratowskiSubdivision();

    interface Embedding<V, E> {
        List<E> getEdgesAround(V vertex);
    }

    class EmbeddingImpl<V, E> implements Embedding<V, E> {
        private Map<V, List<E>> embeddingMap;

        public EmbeddingImpl(Map<V, List<E>> embeddingMap) {
            this.embeddingMap = embeddingMap;
        }

        @Override
        public List<E> getEdgesAround(V vertex) {
            return embeddingMap.get(vertex);
        }
    }
}
