package com.jzel;

import java.util.List;
import lombok.Data;

@Data
public class Structure {

  private String kind;
  private List<List<String>> hold;
}