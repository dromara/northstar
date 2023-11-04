import torch

def set_device():
    # set device to cpu or cuda
    device = torch.device('cpu')
    if(torch.cuda.is_available()): 
        device = torch.device('cuda:0') 
        torch.cuda.empty_cache()
        print(f"Device set to : {str(torch.cuda.get_device_name(device))}")
    else:
        print("Device set to : cpu")
    return device