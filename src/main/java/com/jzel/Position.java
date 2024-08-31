package com.jzel;

public record Position(String kind, Pos pos, Pos velocity) {

  record Pos(double x, double y, double angle) {

  }
}
