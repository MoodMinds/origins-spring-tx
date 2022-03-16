package org.moodminds.spring.transaction.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.moodminds.lang.Emittable;
import org.moodminds.lang.Traversable;
import org.moodminds.lang.TraverseSupport;
import org.moodminds.util.Traverser;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Method;

import static java.util.Optional.ofNullable;
import static org.moodminds.lang.Emittable.emittable;
import static org.moodminds.util.Sneaky.sneaky;
import static org.springframework.aop.support.AopUtils.getTargetClass;
import static org.springframework.util.ClassUtils.getQualifiedMethodName;

/**
 * The {@link Emittable} and {@link Traversable} transactional interceptor.
 */
public class TraverseSupportTransactionInterceptor extends TransactionInterceptor {

    private final TransactionInterceptor transactionInterceptor;

    /**
     * Construct the interceptor with the specified dependencies.
     *
     * @param transactionManager the specified transaction manager instance
     * @param transactionAttributeSource the specified transaction attribute source instance
     * @param beanFactory the specified bean factory instance
     * @param transactionInterceptor the specified transaction interceptor instance
     */
    public TraverseSupportTransactionInterceptor(TransactionManager transactionManager, TransactionAttributeSource transactionAttributeSource,
                                                 BeanFactory beanFactory, TransactionInterceptor transactionInterceptor) {
        super(transactionManager, transactionAttributeSource);
        this.setBeanFactory(beanFactory);
        this.transactionInterceptor = transactionInterceptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Class<?> returnType = method.getReturnType();
        boolean isTraversable = Traversable.class.equals(returnType);
        boolean isEmittable = Emittable.class.equals(returnType);
        if (isTraversable || isEmittable) {
            Class<?> target = invocation.getThis() != null ? getTargetClass(invocation.getThis()) : null;
            TransactionAttribute transactionAttribute = ofNullable(this.getTransactionAttributeSource())
                    .map(attributeSource -> attributeSource.getTransactionAttribute(method, target)).orElse(null);
            TransactionManager transactionManager = this.determineTransactionManager(transactionAttribute);
            if (transactionManager instanceof PlatformTransactionManager) {
                TraverseSupport<?, ?> traverseSupport = (TraverseSupport<?, ?>) invocation.proceed();
                if (traverseSupport != null) {
                    PlatformTransactionManager platformTransactionManager = (PlatformTransactionManager) transactionManager;
                    Traversable<?, ?> traversable = new TransactionalTraversable<>(method, target, traverseSupport, platformTransactionManager, transactionAttribute);
                    return isEmittable ? emittable(traversable) : traversable;
                } else {
                    return null;
                }
            }
        }
        return this.transactionInterceptor.invoke(invocation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionManager getTransactionManager() {
        return this.transactionInterceptor.getTransactionManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionAttributeSource getTransactionAttributeSource() {
        return this.transactionInterceptor.getTransactionAttributeSource();
    }

    private String methodIdentification(Method method, Class<?> targetClass, TransactionAttribute txAttr) {
        String identification = this.methodIdentification(method, targetClass);
        if (identification == null) {
            if (txAttr instanceof DefaultTransactionAttribute)
                identification = ((DefaultTransactionAttribute) txAttr).getDescriptor();
            if (identification == null)
                identification = getQualifiedMethodName(method, targetClass);
        }
        return identification;
    }

    /**
     * The transactional {@link Traversable} implementation.
     *
     * @param <V> the type of the emitting values
     * @param <E> the type of possible exception that might be thrown
     */
    class TransactionalTraversable<V, E extends Exception> implements Traversable<V, E> {

        final Method method;
        final Class<?> target;
        final TraverseSupport<V, E> traverseSupport;
        final PlatformTransactionManager transactionManager;
        final TransactionAttribute transactionAttribute;

        /**
         * Construct the interceptor object with the specified dependencies.
         *
         * @param method the transactional method reflection object
         * @param target the type of the transactional service object
         * @param traverseSupport the specified {@link TraverseSupport} to wrap in transaction
         * @param transactionManager the specified transaction manager object
         * @param transactionAttribute the specified transaction attribute object
         */
        TransactionalTraversable(Method method, Class<?> target, TraverseSupport<V, E> traverseSupport,
                                 PlatformTransactionManager transactionManager, TransactionAttribute transactionAttribute) {
            this.method = method;
            this.target = target;
            this.traverseSupport = traverseSupport;
            this.transactionManager = transactionManager;
            this.transactionAttribute = transactionAttribute;
        }

        @Override
        public <H extends Exception> boolean sequence(Traverser<V, H> traverser) throws E, H {
            TransactionInfo transactionInfo = TraverseSupportTransactionInterceptor.this.createTransactionIfNecessary(this.transactionManager, this.transactionAttribute,
                    TraverseSupportTransactionInterceptor.this.methodIdentification(method, target, transactionAttribute));

            boolean result;

            try {
                result = this.traverseSupport.sequence(traverser);
            } catch (Throwable ex) {
                TraverseSupportTransactionInterceptor.this.completeTransactionAfterThrowing(transactionInfo, ex);
                return sneaky(ex);
            } finally {
                TraverseSupportTransactionInterceptor.this.cleanupTransactionInfo(transactionInfo);
            }

            TraverseSupportTransactionInterceptor.this.commitTransactionAfterReturning(transactionInfo);

            return result;
        }
    }
}
