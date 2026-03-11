.PHONY: deploy test clean

# Deploys all Choragi microservices to Google Cloud Run
deploy:
	bash deploy_cloud_run.sh

# Verifies that all deployed services are live and responding
test:
	bash test_deploy.sh

# Cleans all compiled Java targets in the local environment
clean:
	mvn clean