package org.wildfly.cli.rest.client;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;

@Interceptor
@ExitingOnError
public class ExitingOnErrorInterceptor implements Serializable {
    @AroundInvoke
    public Object auditMethodEntry(InvocationContext ctx) throws Exception {
        try {
            return ctx.proceed();
        } catch (ClientHeaderErrorException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return null;
    }

}
