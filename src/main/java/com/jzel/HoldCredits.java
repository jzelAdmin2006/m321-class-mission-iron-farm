package com.jzel;

public record HoldCredits(Hold hold) {

  record Hold(int credits) {

  }
}
