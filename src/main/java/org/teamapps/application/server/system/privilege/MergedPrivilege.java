package org.teamapps.application.server.system.privilege;

import org.teamapps.icons.Icon;
import org.teamapps.ux.component.template.BaseTemplateTreeNode;

public class MergedPrivilege extends BaseTemplateTreeNode<Object> {

	private final MergedPrivilegeGroup parent;
	private final String name;
	private final Icon icon;
	private final String title;

	public MergedPrivilege(MergedPrivilegeGroup parent, String name, Icon icon, String title) {
		this.parent = parent;
		this.name = name;
		this.icon = icon;
		this.title = title;
	}

	public String getName() {
		return name;
	}

	public Icon getIcon() {
		return icon;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public BaseTemplateTreeNode<Object> getParent() {
		return parent;
	}

	@Override
	public String getCaption() {
		return title;
	}
}
