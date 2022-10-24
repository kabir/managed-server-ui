package org.wildfly.managed.repo;

import org.wildfly.managed.common.model.Application;


public abstract class ApplicationConfigAccessor {
    protected final Application application;

    protected ApplicationConfigAccessor(Application application) {
        this.application = application;
    }

    void setConfig(String contents) {
        // TODO validate contents
        setConfigField(contents);
        if (contents != null) {
            setHasConfigField(true);
        } else {
            setHasConfigField(false);
        }
    }

    protected abstract void setHasConfigField(boolean hasConfig);

    protected abstract void setConfigField(String contents);

    protected abstract String getConfig();

    static ApplicationConfigAccessor getAccessor(Application application, String type) {
        switch (type) {
            case "xml":
                return new XmlConfigAccessor(application);
            case "cli":
                return new CliConfigAccessor(application);
            case "yml":
                return new YmlConfigAccessor(application);
        }
        throw new IllegalStateException("Unknown type");
    }


    private static class XmlConfigAccessor extends ApplicationConfigAccessor {

        public XmlConfigAccessor(Application application) {
            super(application);
        }

        @Override
        protected void setHasConfigField(boolean hasConfig) {
            application.hasServerConfigXml = hasConfig;
        }

        @Override
        protected void setConfigField(String contents) {
            application.serverConfigXml = contents;
        }

        @Override
        protected String getConfig() {
            return application.serverConfigXml;
        }
    }

    private static class CliConfigAccessor extends ApplicationConfigAccessor {

        public CliConfigAccessor(Application application) {
            super(application);
        }

        @Override
        protected void setHasConfigField(boolean hasConfig) {
            application.hasServerInitCli = hasConfig;
        }

        @Override
        protected void setConfigField(String contents) {
            application.serverInitCli = contents;
        }

        @Override
        protected String getConfig() {
            return application.serverInitCli;
        }
    }

    private static class YmlConfigAccessor extends ApplicationConfigAccessor {

        public YmlConfigAccessor(Application application) {
            super(application);
        }

        @Override
        protected void setHasConfigField(boolean hasConfig) {
            application.hasServerInitYml = hasConfig;
        }

        @Override
        protected void setConfigField(String contents) {
            application.serverInitYml = contents;
        }

        @Override
        protected String getConfig() {
            return application.serverInitYml;
        }
    }
}
