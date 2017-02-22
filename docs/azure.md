# Key generation

I'm running the SSH key generation in "Bash for Windows", because puttygen.exe is some GUI dialog which ignores my command line args :-)

- [puttygen command line (on Linux)](https://linux.die.net/man/1/puttygen)

```bash
bash
cd /mnt/c/Users/chgeuer/Java/keys

# sudo apt-get install -y putty-tools

puttygen -O private         -o dcos.putty.ppk       -t rsa -b 2048 -C "chgeuer-dcos-1"
puttygen -O public-openssh  -o dcos.openssh.public  dcos.putty.ppk
puttygen -O private-openssh -o dcos.openssh.private dcos.putty.ppk
```

## Load private key into Putty's pageant agent

```cmd
SET SSH_KEY_DIR=%USERPROFILE%\Java\keys
SET SSH_OPENSSH_PUBLIC_FILE=%SSH_KEY_DIR%\dcos.openssh.public
SET SSH_PUTTY_FILE=%SSH_KEY_DIR%\dcos.putty.ppk
pageant.exe %SSH_PUTTY_FILE%
```

## Download DC/OS client

```cmd
REM Download dcos.exe
curl --output %USERPROFILE%\bin\dcos.exe --get https://downloads.dcos.io/binaries/cli/windows/x86-64/dcos-1.8/dcos.exe
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
SET FLINK_MASTER_COUNT=3
SET FLINK_AGENT_COUNT=3

SET /p SSH_KEY=<%SSH_OPENSSH_PUBLIC_FILE%

az login
az account set --subscription %AZURE_SUBSCRIPTION_NAME%
az group create --name %RESOURCE_GROUP% --location %RESOURCE_GROUP_LOCATION%

SET TEMPLATE_PARAMS="{ \"parameters\": { \"linuxAdminUsername\": { \"value\": \"%USERNAME%\" }, \"sshRSAPublicKey\": { \"value\": \"%SSH_KEY%\" }, \"dnsNamePrefix\": { \"value\": \"%DNS_NAME%\" }, \"orchestratorType\": { \"value\": \"DCOS\" }, \"agentCount\": { \"value\": %FLINK_AGENT_COUNT% }, \"masterCount\": { \"value\": %FLINK_MASTER_COUNT% }, \"agentVMSize\": { \"value\": \"%MACHINE_SIZE%\" } } }"
az group deployment create -g %RESOURCE_GROUP% -n "My_first_DCOS_Deployment" --template-uri %TEMPLATE_URL% --parameters %TEMPLATE_PARAMS% > resource_group_creation_result.json

REM ###### extract the 
type resource_group_creation_result.json | jq .properties.outputs.masterFQDN.value > "masterFQDN.txt"
set /p masterFQDN=<"masterFQDN.txt"
del "masterFQDN.txt"

SET LOCAL_DCOS_PORT=5110

putty -ssh -2 -l %USERNAME% -L %LOCAL_DCOS_PORT%:localhost:80 -P 2200 -agent -A -i %SSH_PUTTY_FILE% %masterFQDN% 
dcos config set core.dcos_url http://localhost:%LOCAL_DCOS_PORT%
```

# Links

- [Deploy an Azure Container Service cluster](https://docs.microsoft.com/en-us/azure/container-service/container-service-deployment)
- [azure-quickstart-templates - ACS DC/OS Template](https://github.com/Azure/azure-quickstart-templates/tree/master/101-acs-dcos)
