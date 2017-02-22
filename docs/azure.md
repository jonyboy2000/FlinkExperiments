
# Key generation

```bash
# sudo apt-get install -y putty-tools

puttygen -O private         -o dcos.putty.ppk       -t rsa -b 2048 -C "chgeuer-dcos-1"
puttygen -O public-openssh  -o dcos.openssh.public  dcos.putty.ppk
puttygen -O private-openssh -o dcos.openssh.private dcos.putty.ppk
```

## Load private key into Putty's pageant agent

```cmd
pageant.exe C:\Users\chgeuer\Java\keys\dcos.private.ppk
```

## Download DC/OS

```bash
REM Download dcos.exe
curl --output %userprofile%\bin\dcos.exe --get https://downloads.dcos.io/binaries/cli/windows/x86-64/dcos-1.8/dcos.exe
```

# Login to Azure

```bash
SET AZURE_SUBSCRIPTION_NAME=chgeuer-work
SET RESOURCE_GROUP=dcos1
SET RESOURCE_GROUP_LOCATION=westeurope

SET TEMPLATE_URL=https://raw.githubusercontent.com/Azure/azure-quickstart-templates/443fdcb1150df42b50140872040e0f902380d4d4/101-acs-dcos/azuredeploy.json

SET USERNAME=chgeuer
SET DNS_NAME=chgeuerdcos1
SET MACHINE_SIZE=Standard_A2
SET SSH_KEY=ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAQEArw2tuf6LfyUxz8LOWRooAnZXtJsJkPgtjrQrVhpLTnHCYzM11faqslDno1Y+qbo1riPiPGJs6VQCITbzDfXE7EfSs7kPNE70csZJedIzTmTIzujdhwCZGWamacdSwBAulJnamdY//QU5ZwXv8PXge6qPpzEfAhV7vo+QQDDMWLLBqfPMT3wycpeH7iEivXrd9bBhmtvSr3d28DhLkEKUEg/S0Qu2CSJDYyPf1btJX3SxConxEiu7rPUGwPEPwy1p1kDVEyeoJ1cx2tqoWYvqfiZOO7K4DdpgI01vPIq6orRnM6QXkYMF1Gg5fqxwO8kWQja8aVbkTeX3akbfxtNdGQ== chgeuer-dcos-1


SET TEMPLATE_PARAMS="{ \"parameters\": { \"linuxAdminUsername\": { \"value\": \"%USERNAME%\" }, \"sshRSAPublicKey\": { \"value\": \"%SSH_KEY%\" }, \"dnsNamePrefix\": { \"value\": \"%DNS_NAME%\" }, \"orchestratorType\": { \"value\": \"DCOS\" }, \"agentCount\": { \"value\": 1 }, \"masterCount\": { \"value\": 3 }, \"agentVMSize\": { \"value\": \"%MACHINE_SIZE%\" } } }"

az login
az account set --subscription %AZURE_SUBSCRIPTION_NAME%
az group create --name %RESOURCE_GROUP% --location %RESOURCE_GROUP_LOCATION%
az group deployment create -g %RESOURCE_GROUP% -n "My_first_DCOS_Deployment" --template-uri %TEMPLATE_URL% --parameters %TEMPLATE_PARAMS% > out.json
type out.json | jq .properties.outputs.sshMaster0.value

SET LOCAL_DCOS_PORT=5110

putty -ssh -2 -l %USERNAME% -L %LOCAL_DCOS_PORT%:localhost:80 -P 2200 -agent -A -i C:\Users\chgeuer\Java\keys\dcos.putty.ppk chgeuerdcos1mgmt.westeurope.cloudapp.azure.com 
dcos config set core.dcos_url http://localhost:%LOCAL_DCOS_PORT%


```

# Links

- [Deploy an Azure Container Service cluster](https://docs.microsoft.com/en-us/azure/container-service/container-service-deployment)
- [puttygen command line](https://linux.die.net/man/1/puttygen)
- [azure-quickstart-templates - ACS DC/OS Template](https://github.com/Azure/azure-quickstart-templates/tree/master/101-acs-dcos)
