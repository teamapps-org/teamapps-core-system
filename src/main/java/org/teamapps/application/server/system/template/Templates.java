/*-
 * ========================LICENSE_START=================================
 * TeamApps Application Server
 * ---
 * Copyright (C) 2020 - 2022 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.application.server.system.template;

import org.teamapps.common.format.Color;
import org.teamapps.dto.UiTemplate;
import org.teamapps.dto.UiTemplateReference;
import org.teamapps.ux.component.format.*;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.template.Template;
import org.teamapps.ux.component.template.gridtemplate.*;
import org.teamapps.ux.component.template.htmltemplate.MustacheTemplate;

import java.util.List;

public enum Templates implements Template {

	LOGIN_TEMPLATE(createLoginTemplate()),
	LIST_4_LINES_TEMPLATE(create4LinesTemplate()),
	NODE_TEMPLATE(createNodeTemplate()),
	ORGANIZATION_GRAPH_TEMPLATE(createOrganizationGraphTreeNodeTpl()),
	ORGANIZATION_GRAPH_SMALL_BlUE_TEMPLATE(createOrganizationGraphTreeNodeSmallTpl(Color.MATERIAL_BLUE_900)),
	ORGANIZATION_GRAPH_SMALL_GREEN_TEMPLATE(createOrganizationGraphTreeNodeSmallTpl(Color.MATERIAL_GREEN_900)),
	ORGANIZATION_GRAPH_SMALL_GREY_TEMPLATE(createOrganizationGraphTreeNodeSmallTpl(Color.MATERIAL_GREY_900)),
	;

	public final static String PROPERTY_IMAGE = BaseTemplate.PROPERTY_IMAGE;
	public final static String PROPERTY_ICON = BaseTemplate.PROPERTY_ICON;
	public final static String PROPERTY_BADGE = BaseTemplate.PROPERTY_BADGE;
	public final static String PROPERTY_CAPTION = BaseTemplate.PROPERTY_CAPTION;
	public final static String PROPERTY_DESCRIPTION = BaseTemplate.PROPERTY_DESCRIPTION;
	public final static String PROPERTY_LINE3 = "line3";
	public final static String PROPERTY_LINE4 = "line4";
	public final static String PROPERTY_LINE1_LEFT_ICON = "lin1LeftIcon";
	public final static String PROPERTY_BORDER_COLOR = "borderColor";

	private final Template template;
	private final UiTemplateReference uiTemplateReference;

	public static MustacheTemplate createNodeTemplate() {
		String tpl =
				"<div style=\"background-image: url({{" + PROPERTY_IMAGE + "}}); top:-17px; left:-60px; position: absolute;width: 80px; height: 80px;background-size: cover; background-position: center center; margin-bottom: 4px; border-radius: 50%; border: 2px solid {{" + PROPERTY_BORDER_COLOR + "}}; \"></div>\n" +
						"<div style='width:100%;height:100%; padding-left: 24px; padding-top: 5px; overflow: hidden;'>\n" +
						"<div style=\"white-space: nowrap;\">{{" + PROPERTY_CAPTION + "}}</div>\n" +
						"<div style=\"white-space: nowrap;\">{{" + PROPERTY_DESCRIPTION + "}}</div>\n" +
						"<div style=\"white-space: nowrap;\">{{" + PROPERTY_LINE3 + "}}</div>\n" +
						"<div style=\"white-space: nowrap; width: fit-content; border-radius: 1000px; color:rgba(66,66,66,1);background-color:rgba(238,238,238,1);font-size:60%; padding: 1px 5px;\">{{" + PROPERTY_BADGE + "}}</div>\n" +
						"</div>";
		return new MustacheTemplate(tpl);
	}

	public static MustacheTemplate createLoginTemplate() {
		String tpl =
				"<div class=\"token-login-entry\">\n" +
						"    <style>\n" +
						"        .token-login-entry {\n" +
						"            background: transparent !important;\n" +
						"            border: none !important;\n" +
						"            box-shadow: none !important;\n" +
						"            font-family: \"Helvetica Neue\", Helvetica, Arial, sans-serif;\n" +
						"            display: flex;\n" +
						"            flex-direction: column;\n" +
						"            align-items: center;\n" +
						"            transform: scale(1);\n" +
						"        }\n" +
						"        .token-login-entry:hover {\n" +
						"            transform: scale(1.2);\n" +
						"        }\n" +
						"        .token-login-entry, .token-login-entry:hover {\n" +
						"            transition: transform .3s !important;\n" +
						"        }\n" +
						"        .token-login-entry > .img {\n" +
						"            width: 100px;\n" +
						"            height: 100px;\n" +
						"            background-size: cover;\n" +
						"            background-position: center center;\n" +
						"            margin-bottom: 6px;\n" +
						"            border-radius: 50%;\n" +
						"            border: 1px solid #fff;\n" +
						"            box-shadow: 0 3px 10px 0 #000000a1;\n" +
						"        }\n" +
						"        .token-login-entry > .line1,\n" +
						"        .token-login-entry > .line2 {\n" +
						"            text-align: center;\n" +
						"            font-size: 105%;\n" +
						"            text-overflow: ellipsis;\n" +
						"            overflow: hidden;\n" +
						"        }\n" +
						"    </style>\n" +
						"    <div class=\"img\" style=\"background-image: url('{{" + PROPERTY_IMAGE + "}}');\"></div>\n" +
						"    <div class=\"line1\">{{" + PROPERTY_CAPTION + "}}</div>\n" +
						"    <div class=\"line2\">{{#description}}{{description}}{{/description}}{{^description}}&nbsp;{{/description}}</div>\n" +
						"</div>";
		return new MustacheTemplate(tpl);
	}

	public static Template createOrganizationGraphTreeNodeTpl() {
		GridTemplate tpl = new GridTemplate()
				.setPadding(new Spacing(5, 1, 1, 40))
				.addColumn(SizingPolicy.fixed(240))
				.addRow(SizeType.FIXED, 18, 18, 0, 0)
				.addRow(SizeType.FIXED, 16, 16, 0, 0)
				.addRow(SizeType.FIXED, 16, 16, 0, 0)
				.addElement(new TextElement(PROPERTY_CAPTION, 0, 0)
						.setWrapLines(false)
						.setFontStyle(new FontStyle(1.2f, Color.MATERIAL_BLUE_900, null, false, false, false))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setHorizontalAlignment(HorizontalElementAlignment.LEFT))
				.addElement(new TextElement(PROPERTY_DESCRIPTION, 1, 0)
						.setWrapLines(false)
						.setFontStyle(new FontStyle(1f, Color.MATERIAL_GREY_900, null, true, false, false))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setHorizontalAlignment(HorizontalElementAlignment.LEFT))
				.addElement(new TextElement(PROPERTY_LINE3, 2, 0)
						.setWrapLines(false)
						.setFontStyle(new FontStyle(1f, Color.MATERIAL_GREY_700, null, false, false, false))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setHorizontalAlignment(HorizontalElementAlignment.LEFT)
				);
		return tpl;
	}

	public static Template createOrganizationGraphTreeNodeSmallTpl(Color textColor) {
		GridTemplate tpl = new GridTemplate()
				.setPadding(new Spacing(2, 1, 1, 30))
				.addColumn(SizingPolicy.fixed(200))
				.addRow(SizeType.FIXED, 16, 16, 0, 0)
				.addRow(SizeType.FIXED, 16, 16, 0, 0)
				.addElement(new TextElement(PROPERTY_DESCRIPTION, 0, 0)
						.setWrapLines(false)
						.setFontStyle(new FontStyle(1f, textColor, null, true, false, false))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setHorizontalAlignment(HorizontalElementAlignment.LEFT))
				.addElement(new TextElement(PROPERTY_LINE3, 1, 0)
						.setWrapLines(false)
						.setFontStyle(new FontStyle(1f, Color.MATERIAL_GREY_700, null, false, false, false))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setHorizontalAlignment(HorizontalElementAlignment.LEFT));
		return tpl;
	}


	public static Template create4LinesTemplate() {
		GridTemplate tpl = new GridTemplate()
				.setPadding(new Spacing(1))
				.setGridGap(0)
				.addColumn(SizingPolicy.AUTO)
				.addColumn(SizingPolicy.FRACTION)
				.addRow(SizeType.FIXED, 18, 18, 0, 0)
				.addRow(SizeType.FIXED, 18, 18, 0, 0)
				.addRow(SizeType.FIXED, 18, 18, 0, 0)
				.addRow(SizeType.FIXED, 18, 18, 0, 0)
				.addElement(new ImageElement(PROPERTY_IMAGE, 0, 0, 68, 68).setRowSpan(4)
						.setBorder(new Border(new Line(Color.GRAY, LineType.SOLID, 0.5f)).setBorderRadius(300))
						//.setShadow(Shadow.withSize(0.5f))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setMargin(new Spacing(0, 8, 0, 4)))
				.addElement(new IconElement(PROPERTY_ICON, 0, 0, 64).setRowSpan(4)
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setMargin(new Spacing(0, 8, 0, 4)))
				.addElement(new FloatingElement(0, 1)
						.addElement(new IconElement(PROPERTY_LINE1_LEFT_ICON, 0, 0, 16)
								.setMargin(new Spacing(0, 4, 0, 0)))
						.addElement(new TextElement(PROPERTY_CAPTION, 0, 0)
								.setWrapLines(false)
								.setFontStyle(new FontStyle(1f, Color.MATERIAL_GREY_900, null, true, false, false)))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setHorizontalAlignment(HorizontalElementAlignment.LEFT))
				.addElement(new TextElement(PROPERTY_DESCRIPTION, 1, 1)
						.setWrapLines(false)
						.setFontStyle(new FontStyle(1f, Color.MATERIAL_GREY_700, null, false, false, false))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setHorizontalAlignment(HorizontalElementAlignment.LEFT))
				.addElement(new TextElement(PROPERTY_LINE3, 2, 1)
						.setWrapLines(false)
						.setFontStyle(new FontStyle(1f, Color.MATERIAL_GREY_700, null, false, false, false))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setHorizontalAlignment(HorizontalElementAlignment.LEFT))
				.addElement(new TextElement(PROPERTY_LINE4, 3, 1)
						.setWrapLines(false)
						.setFontStyle(new FontStyle(1f, Color.MATERIAL_BLUE_900, null, true, false, false))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setHorizontalAlignment(HorizontalElementAlignment.LEFT));
		tpl.setBorder(new Border(new Line(Color.MATERIAL_GREY_300, LineType.SOLID, 0.5f), null, null, null));
		return tpl;
	}

	private Templates(Template template) {
		this.template = template;
		this.uiTemplateReference = new UiTemplateReference(this.name());
	}

	public UiTemplate createUiTemplate() {
		return this.uiTemplateReference;
	}
	
	@Override
	public List<String> getPropertyNames() {
		return this.template.getPropertyNames();
	}

	public Template getTemplate() {
		return this.template;
	}
}
