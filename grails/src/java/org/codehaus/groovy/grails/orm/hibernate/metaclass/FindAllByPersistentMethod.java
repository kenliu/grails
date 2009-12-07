package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import groovy.lang.Closure;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The "findBy*" static persistent method. This method allows querying for
 * instances of grails domain classes based on their properties. This method returns a list of all found results
 * 
 * 
 * eg.
 * Account.findAllByHolder("Joe Blogs"); // Where class "Account" has a property called "holder"
 * Account.findAllByHolderAndBranch("Joe Blogs", "London" ); // Where class "Account" has a properties called "holder" and "branch"
 * 
 * @author Graeme Rocher
 * @since 13-Dec-2005
 *
 */
public class FindAllByPersistentMethod extends
		AbstractClausedStaticPersistentMethod {

	private static final String OPERATOR_OR = "Or";
	private static final String OPERATOR_AND = "And";
	
	private static final String METHOD_PATTERN = "(findAllBy)(\\w+)";
	private static final String[] OPERATORS = new String[]{ OPERATOR_AND, OPERATOR_OR };

	public FindAllByPersistentMethod(GrailsApplication application, SessionFactory sessionFactory, ClassLoader classLoader) {
		super(application, sessionFactory, classLoader, Pattern.compile(METHOD_PATTERN), OPERATORS);
	}

	protected Object doInvokeInternalWithExpressions(final Class clazz,
                                                     String methodName, final Object[] arguments, final List expressions, String operatorInUse, final Closure additionalCriteria) {

        final String operator = OPERATOR_OR.equals(operatorInUse) ? OPERATOR_OR : OPERATOR_AND;
        
        return super.getHibernateTemplate().executeFind( new HibernateCallback() {

			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				final Criteria c = getCriteria(session, additionalCriteria, clazz);

                Map argsMap = (arguments.length > 0 && (arguments[0] instanceof Map)) ? (Map) arguments[0] : Collections.EMPTY_MAP;
                GrailsHibernateUtil.populateArgumentsForCriteria(clazz, c,argsMap);
								
                if(operator.equals(OPERATOR_OR)) {
                    Disjunction dis = Restrictions.disjunction();
                    if(firstExpressionIsRequiredBoolean()) {
                        GrailsMethodExpression expression = (GrailsMethodExpression) expressions.remove(0);
                        c.add(expression.getCriterion());
                    }
                    for (Object expression : expressions) {
                        GrailsMethodExpression current = (GrailsMethodExpression) expression;
                        dis.add(current.getCriterion());
                    }
                    c.add(dis);
                }
                else {
                    for (Object expression : expressions) {
                        GrailsMethodExpression current = (GrailsMethodExpression) expression;
                        c.add(current.getCriterion());

                    }
                }

                c.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				return c.list();
			}
		});
	}

    /**
     * Indicates if the first expression in the query is a required boolean property and as such should
     * be ANDed to the other expressions, not ORed.
     *
     * @return true if the first expression is a required boolean property, false otherwise
     * @see org.codehaus.groovy.grails.orm.hibernate.metaclass.FindAllByBooleanPropertyPersistentMethod
     */
    protected boolean firstExpressionIsRequiredBoolean() {
        return false;
    }
}
