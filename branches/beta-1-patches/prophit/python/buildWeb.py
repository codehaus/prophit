import sys
from xml.dom import Node
from xml import xpath
from xml.dom.ext.reader import Sax2

HEADER = \
'''
<html>
<head>
<title>@@name@@</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" href="default.css" type="text/css">
</head>
<body class="normal" bgcolor="#FFFFFF" text="#000000">
<h1 align="center">@@name@@</h1>
'''

NAVIGATION = \
'''
<table border='0' cellspacing='0' cellpadding='0' bgcolor='eeeeee'>
  <tr> 
    <td width='179' nowrap>Previous : @@previous-link@@</td>
    <td width='20' bgcolor='ffffff' nowrap>&nbsp;</td>
    <td width='157' nowrap>Up : @@up-link@@</td>
    <td width='20' bgcolor='ffffff' nowrap>&nbsp;</td>
    <td width='234' nowrap>Next : @@next-link@@</td>
  </tr>
</table>
'''

#subsection = \
#"""<li><a href='hello-list-prof-sample.html'>HelloList <code>java -prof </code>Profile</a></li>"""

FOOTER = \
"""
</body>
</html>
"""

if len(sys.argv) < 3:
	print "usage : buildWeb.py <template> <outputdir>"
	print "  template - should be an instance of site.dtd in the current working directory"
	sys.exit(1)
	
templateFile = open(sys.argv[1])
outputDir = sys.argv[2]

reader = Sax2.Reader()
doc = reader.fromStream(templateFile)
site = doc.documentElement

def getPages(list):
	return filter(lambda node:
				  node.nodeType == Node.ELEMENT_NODE and node.tagName == 'page', list)

def getName(page):
	names = filter(lambda node:
				  node.nodeType == Node.ELEMENT_NODE and node.tagName == 'name', page.childNodes)
	if len(names):
		return names[0].childNodes[0].nodeValue
	else:
		return ""

def xpathSingleNode(expr, context):
	result = xpath.Evaluate(expr, context)
	if len(result):
		return result[0]
	else:
		return None

def printPages(parent):
	for page in getPages(parent.childNodes):
		if page.getAttribute('templated') != 'false':
			fileName = page.getAttribute('file')
			name = getName(page)
			print 'Processing %s [ %s ]' % ( name, fileName )
			result = open(outputDir + '/' + fileName, 'w')
			parentPage = xpathSingleNode('ancestor::page', page)
			previousPage = xpathSingleNode('preceding-sibling::page[position()=1]', page)
			nextPage = xpathSingleNode('following-sibling::page[position()=1]', page)

			if not previousPage:
				previousPage = parentPage

			fileContents = ''
			try:
				file = open(fileName)
				fileContents = file.read(-1)
			except IOError:
				print 'Unable to open file %s' % fileName
			
			header = HEADER.replace('@@name@@', name)
			navigation = NAVIGATION.replace('@@previous-link@@', '<a href="%s">%s</a>' % ( previousPage.getAttribute('file'), getName(previousPage) ))
			navigation = navigation.replace('@@up-link@@', '<a href="%s">%s</a>' % ( parentPage.getAttribute('file'), getName(parentPage) ))
			if nextPage:
				navigation = navigation.replace('@@next-link@@', '<a href="%s">%s</a>' % ( nextPage.getAttribute('file'), getName(nextPage) ))
			else:
				navigation = navigation.replace('@@next-link@@', '')

			result.write(header)
			result.write(navigation)
			result.write(fileContents)

			result.write( \
"""
<h1>Subsections</h1>
<ul>
""")
			for child in getPages(page.childNodes):
				result.write('\t<li><a href="%s">%s</a></li>\n' % ( child.getAttribute('file'), getName(child) ) )
			result.write( \
"""
</ul>
""")
			
			result.write(FOOTER)
			result.close()
			
		printPages(page)
	
printPages(site)
	
