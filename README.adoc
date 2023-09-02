
#  Building Secure Systems and Services with the Spring Authorization Server, OAuth, and Spring Boot

Hi, Spring fans! In this installment we're going to look at some patterns and practices for working with the Spring Authorization Server, and with OAuth in general in a Spring Boot-based system. 

The Spring Authorization Server is one of my favorite new projects. It's a full blown OAuth identity provider (IDP), distributed as Spring Boot autoconfiguration.

The Spring Authorization Server is the final piece in the Spring Security OAuth's Ship of Theseus, the final replacement for a component in the built-in OAuth support in Spring Security 5 and later. First there was OIDC client support, and then resource server support, and now - after a _lot_ of community outpouring and support  - a brand new and fully featured OAuth IDP. 

Why not use Keycloak or Okta or Auth0, I hear you ask?  And the answer is.. be our guest! 99% of the stuff we're going to look at here works with any OAuth IDP. But i do so love the Spring Authorization Server. I don't know an easier way to get as configurable and flexible IDP up-and-running. 

Having an easy component for your IDP integration is _liberating_. If nothign else, it's one less `users` microservice for you to build. But, at its best, its a unifying force for your organization's notions of identity and policy, all centralized. If you implement it correctly, it's one less `users` microservice for _all_ of your systems, not just the one you're working on now! 

It's Spring, so it of course supports the new-and-novel, but it's also built like all Spring Boot autoconfiguration, with hooks and customization in mind at every step.  

I love it and I love it for helping me to love OAuth. If you only knew the agony I've been subjhected to in learing and weilding OAuth over the years, then you'll know what a thing that is for me to say.

And I want you, dear reader, to love OAuth. And the way to do that is to, basically, not care about OAuth. And so we're going to take a journey to production together, with the Spring Authorization Server at our backs, and learn how to weild OAuth (via the amazing Spring Authorization Server) for some common kinds of patterns.


## Try it out! 

Now, before we go too far, try it out! 

* in root, run `docker compose up`
* in `authorization-service`, run `run.sh`
* in `gateway`, run `run.sh`
* in `api`, run `run.sh`
* in `processor`, run `run.sh`
* in `static`, run `run.sh`
* visit `http://127.0.0.1:8082` (important: use the IP, _not_ `localhost`!) in the browser.
* login with `jlong`/`password` (yes, I know it's a terrible password, don't `@` me!), and then consent when prompted.
* you'll see a list of customers, click on the `email` button to kick off work in the `processor`. You should see indications in the console that your message has been sent.

Lot's of moving parts, but here's what you need to know: we have a JavaScript/HTML 5 client, a backend HTTP API, and a headless backoffice process, all of which have been secured with the Spring Authorization Server.

Refreshingly simple. Let's dive right into the nitty gritty. I want you building secure systems by the article's end. My goal here is not to cover _every_ possible use case, but to cover some of the typical usecases and introduce progressively more moving parts so that you, if at some point you don't see what you need, you know where to reach you to build it yourself. Let's dive right into it! 

## 

















