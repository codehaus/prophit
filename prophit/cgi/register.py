from WebKit.HTTPServlet import HTTPServlet

class register(HTTPServlet):
	
	def respond(self, trans):
		request = trans.request()
		
		formOK = 0
		
		if request.hasValue("name") and request.hasValue("email") \
			   and request.value("name") != "" and request.value("email") != "" and request.value("use") != "":
			formOK = 1
		
		if not formOK:
			trans.response().sendRedirect("/register-failed.html") # error page
		else:
			# write out to two files - one is a user list with all user info
			# the other file is all emails in a return-carriage delimited file.
			
			f = open("/home2/sfrancis/users.txt", 'a')
			f.write("%s:%s:%s:%s:USE:%s\n" % (request.value("name"), request.value("email"), request.value("org"), request.value("phone"), request.value("use")))
			f.close()
			
			f2 = open("/home2/sfrancis/emails.txt", 'a')
			f2.write("%s\n" % request.value("email"))
			f2.close

			# print a response page
			# redirect them to prophit.jnlp (and include a link just in case)
			trans.response().write( "<BODY>" )
			trans.response().write( "<H1>Registration Successful</H1>" )
			trans.response().write( "You have been successfully registered to use prophIt!" )
			trans.response().write( "If this page does not redirect momentarily, click on the link below:" )
			trans.response().write( '<a href="http://prophit.westslopesoftware.com/prophIt.jnlp">http://prophit.westslopesoftware.com/prophIt.jnlp</a>' )
			trans.response().write( "</BODY></HTML>" )
			

			trans.response().sendRedirect("/prophIt.jnlp")
