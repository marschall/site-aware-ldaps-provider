package com.github.marschall.siteawareldapsprovider;

/**
 * Constants for additional environment properties supported by this project.
 */
public final class ActiveDirectoryContext {

  private ActiveDirectoryContext() {
    throw new AssertionError("not instantiable");
  }

  /**
   * Constant that holds the name of the environment property
   * for specifying the Active Directory site to be used.
   */
  public static final String SITE = "com.github.marschall.siteawareldapsprovider.site";

}
