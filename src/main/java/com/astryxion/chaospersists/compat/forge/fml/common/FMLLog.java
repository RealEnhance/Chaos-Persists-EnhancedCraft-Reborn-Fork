package com.astryxion.chaospersists.compat.forge.fml.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Legacy 1.12 {@code FMLLog} for chunk provider warnings. */
public final class FMLLog {
  public static final Logger log = LogManager.getLogger("chaospersists");

  private FMLLog() {}
}
