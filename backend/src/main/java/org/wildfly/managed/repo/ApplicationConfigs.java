package org.wildfly.managed.repo;

public class ApplicationConfigs {
    private final String xml;
    private final String yml;
    private final String cli;

    public ApplicationConfigs(String xml, String yml, String cli) {
        this.xml = xml;
        this.yml = yml;
        this.cli = cli;
    }

    public String getXml() {
        return xml;
    }

    public String getYml() {
        return yml;
    }

    public String getCli() {
        return cli;
    }
}
