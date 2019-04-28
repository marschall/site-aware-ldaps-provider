module com.github.marschall.siteawareldapsprovider {

  requires java.naming;

  exports com.github.marschall.siteawareldapsprovider;

  provides javax.naming.ldap.spi.LdapDnsProvider with com.github.marschall.siteawareldapsprovider.SiteAwareLdapsProvider;

}