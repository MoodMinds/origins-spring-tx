package org.moodminds.spring.transaction;

import org.moodminds.lang.Emittable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.moodminds.spring.transaction.interceptor.TraverseSupportTransactionInterceptor;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * The {@link Emittable} transaction interceptor registration configuration bean.
 */
@Configuration
public class TraverseSupportTransactionAdvisory implements InitializingBean {

    private final BeanFactory beanFactory;
    private final TransactionManager transactionManager;
    private final TransactionInterceptor transactionInterceptor;
    private final TransactionAttributeSource transactionAttributeSource;
    private final BeanFactoryTransactionAttributeSourceAdvisor transactionAttributeSourceAdvisor;

    /**
     * Construct the configuration bean object with the specified dependencies.
     *
     * @param beanFactory the specified bean factory instance
     * @param transactionManager the specified transaction manager instance
     * @param transactionInterceptor the specified transaction interceptor instance
     * @param transactionAttributeSource the specified transaction attribute source instance
     * @param transactionAttributeSourceAdvisor the specified transaction attribute source advisor
     */
    public TraverseSupportTransactionAdvisory(BeanFactory beanFactory, TransactionManager transactionManager,
                                              TransactionInterceptor transactionInterceptor,
                                              TransactionAttributeSource transactionAttributeSource,
                                              BeanFactoryTransactionAttributeSourceAdvisor transactionAttributeSourceAdvisor) {
        this.beanFactory = beanFactory;
        this.transactionManager = transactionManager;
        this.transactionInterceptor = transactionInterceptor;
        this.transactionAttributeSource = transactionAttributeSource;
        this.transactionAttributeSourceAdvisor = transactionAttributeSourceAdvisor;
    }

    /**
     * Register the {@link Emittable} transaction interceptor.
     */
    @Override
    public void afterPropertiesSet() {
        transactionAttributeSourceAdvisor.setAdvice(new TraverseSupportTransactionInterceptor(
                transactionManager, transactionAttributeSource,
                beanFactory, transactionInterceptor)
        );
    }
}
