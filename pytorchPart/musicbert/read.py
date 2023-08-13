#导入所需的包
import numpy as np  

#导入npy文件路径位置
test = np.load('genre.npy',  allow_pickle=True)

print(type(test))

