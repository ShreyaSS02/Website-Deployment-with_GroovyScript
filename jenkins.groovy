job('Job-1') {
    scm {
        github('Shreya-cyber/Devops6','master')
    }
    steps{
        shell('sudo cp -rvf * /root/task')
    triggers {
        upstream('JOB DSL', 'SUCCESS')
    }
    }

    }

//-----------------------------------------------------------JOB-2-------------------------------------------------------------------------

job('JOB-2') {

steps {
        shell('''#!/bin/bash
cd /root/task/

output=$(find -name "*.php")

if [[ $output == *".php"* ]]; then

    kubectl apply -f /root/YML/pvc.yml 
    kubectl apply -f /root/YML/php.yml
    sleep 25
	kubectl get pods --output json > /root/pods.json
	parser=$(jq  -r ".items[]?.status.containerStatuses[]?.ready" /root/pods.json)
	if [[ $parser == *"true"* ]]; then
          trans=$(jq -r  ".items[].metadata.name" /root/pods.json) #parsing 
      	  kubectl cp /root/task/*.php $trans:/var/www/html  #paste file in container 
      	  kubectl apply -f /root/YML/service.yml # service creation 
          kubectl get all --output json > /root/pods.json
      
	else
      echo "NO"
	fi
fi

#optional
jq  -r  ".items[]?.status.containerStatuses[]?.ready" /root/pods.json 
jq  -r  ".items[].metadata.name" /root/pods.json

port=$(jq  -r ".items[2]?.spec.ports[].nodePort" /root/pods.json) #store value
ip=$(jq -r ".items[]?.status.hostIP //empty " /root/pods.json)

echo $ip:$port # pass this into new job ''')
   triggers {
        upstream('Job-1', 'SUCCESS')
    }
    }
}
//----------------------------------------------------------JOB-3------------------------------------------------------------------------

job('JOB-3') {

   

    steps{
        shell('''port=$(jq -r ".items[2]?.spec.ports[].nodePort" /root/pods.json)
ip=$(jq -r ".items[]?.status.hostIP //empty " /root/pods.json)
if curl -s --head --request GET http://$ip:$port | grep "200 OK" > /dev/null; then
   echo "Working"
else
      curl http://192.168.198.128:1999/job/JOB-4/build?token=redhat --user admin:redhat
      exit 1
fi ''')

triggers {
        upstream('JOB-2', 'SUCCESS')
}
    }

    }
//--------------------------------------------------------------JOB-4----------------------------------------------------------------------
job('JOB-4') {
    authenticationToken('redhat')
	  publishers {
	        extendedEmail {
	            recipientList('shreya02santoshwar@gmail.com')
	            defaultSubject('Job status')
	            defaultContent('Status Report')
	            contentType('text/html')
	            triggers {
	                always {
	                    subject('build Status')
	                    content('Body')
	                    sendTo {
	                        developers()
	                        recipientList()
	                    }
			       }
		       }
		   }
	  }
}
// ++++++++++++++++++++++++++++++++++++++++++++++++++BUILD_PIPELINE+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++====
buildPipelineView('project-A') {
    filterBuildQueue()
    filterExecutors()
    title('Project A CI Pipeline')
    displayedBuilds(5)
    selectedJob('Job-1')
    alwaysAllowManualTrigger()
    showPipelineParameters()
    refreshFrequency(60)
}