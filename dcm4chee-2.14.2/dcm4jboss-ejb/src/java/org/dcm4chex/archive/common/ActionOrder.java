package org.dcm4chex.archive.common;

/**
 * This is generic JMS order where a given action is executed against the given data.
 * 
 * @author fang.yang@agfa.com
 * @version $Revision: 2500 $ $Date: 2006-05-31 14:44:06 +0200 (Wed, 31 May 2006) $
 * @since May 3, 2006
 *
 */
public class ActionOrder extends BaseJmsOrder {

	private static final long serialVersionUID = 7625694942213009636L;

	private final String actionMethod;
	private final Object data;
	
	public ActionOrder(String actionMethod)
	{		
		this(actionMethod, null);
	}
	
	public ActionOrder(String actionMethod, Object data)
	{
		this.actionMethod = actionMethod;
		this.data = data;
	}

	/**
	 * Get the method name of the action
	 * @return the method name
	 */
	public String getActionMethod() {
		return actionMethod;
	}

	/**
	 * Get the data for the action. The action method should have knowledge of the type of this data
	 * @return
	 */
	public Object getData() {
		return data;
	}
	
	//@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append("\tAction method: ").append(actionMethod).append("\n");
		if(data != null)
			sb.append("\tData: ").append(data.toString()).append("\n");
		return sb.toString();
	}
}
