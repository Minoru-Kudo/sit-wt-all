package org.sitoolkit.wt.app.config;

import org.apache.commons.beanutils.Converter;
import org.sitoolkit.util.tabledata.BeanFactory;
import org.sitoolkit.util.tabledata.TableDataMapper;
import org.sitoolkit.wt.domain.operation.Operation;
import org.sitoolkit.wt.domain.operation.OperationResult;
import org.sitoolkit.wt.domain.tester.TestContext;
import org.sitoolkit.wt.domain.testscript.Locator;
import org.sitoolkit.wt.domain.testscript.OperationConverter;
import org.sitoolkit.wt.domain.testscript.TestScript;
import org.sitoolkit.wt.domain.testscript.TestStep;
import org.sitoolkit.wt.infra.ELSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

@Configuration
public class TestScriptConfig {

    @Bean
    @Scope("prototype")
    public TestScript testScript() {
        return new TestScript();
    }

    @Bean
    @Scope("prototype")
    public TestStep testStep() {
        return new TestStep();
    }

    @Bean
    public ELSupport elSupport(TestContext testContext) {
        return new ELSupport(testContext);
    }

    @Bean
    @Scope("prototype")
    public Locator locator() {
        return new Locator();
    }

    @Bean
    @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, scopeName = "thread")
    public TestContext testContext() {
        return new TestContext();
    }

    @Bean
    @Primary
    public TableDataMapper tableDataMapper(OperationConverter converter, BeanFactory beanFactory) {
        TableDataMapper tdm = new TableDataMapper();
        tdm.getConverterMap().put(Operation.class, new Converter() {

            @Override
            public <T> T convert(Class<T> type, Object value) {
                return (T) new Operation() {

                    @Override
                    public OperationResult operate(TestStep testStep) {
                        // TODO Auto-generated method stub
                        return null;
                    }
                };
            }
        });
        tdm.setBeanFactory(beanFactory);
        return tdm;
    }

}
