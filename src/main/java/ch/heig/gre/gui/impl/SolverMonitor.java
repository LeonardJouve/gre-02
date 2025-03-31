package ch.heig.gre.gui.impl;

import ch.heig.gre.graph.GraphObserver;
import ch.heig.gre.graph.VertexLabelling;

import java.util.Arrays;

public final class SolverMonitor implements VertexLabelling<Boolean> {
  private final boolean[] labels;
  private final GraphObserver observer;

  public SolverMonitor(int size, GraphObserver observer) {
    this.labels = new boolean[size];
    this.observer = observer;
  }

  @Override
  public Boolean getLabel(int v) {
    return labels[v];
  }

  @Override
  public void setLabel(int v, Boolean label) {
    if (label == labels[v]) return;

    labels[v] = label;
    observer.onVertexChanged(v);
  }
}
