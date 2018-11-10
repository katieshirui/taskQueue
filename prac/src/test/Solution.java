package test;

import java.util.ArrayList;
import java.util.List;

public class Solution {
public void sum2(int[] nums,int target) {
	List<List<Integer>> res=new ArrayList<List<Integer>>();
	List<Integer> list=new ArrayList<Integer>();
	helper2(nums,0,res,list,target);
	System.out.println(1);
}
public void helper2(int[] nums,int pos,List res,List list,int tar) {
	if(pos==nums.length)return;
	if(tar<0)return;
	if(list.size()==2 ) {
		if(tar==0) {
		res.add(new ArrayList(list));
		}
		return;
	}
	list.add(nums[pos]);
	tar-=nums[pos];
	helper2(nums,pos+1,res,list,tar);
	list.remove(list.size()-1);
	tar+=nums[pos];
	helper2(nums,pos+1,res,list,tar);
} 
public List<List<Integer>> subsets(int[] nums) {
	List<List<Integer>> res=new ArrayList<List<Integer>>();
	List<Integer> list=new ArrayList<Integer>();
        helper(nums,0,nums.length-1,res,list);
        return res;
    }
public void helper(int[] nums,int pos,int len,List res,List list) {
	if(pos<=len) {
			list.add(nums[pos]);
			res.add(new ArrayList(list));
			helper(nums,pos+1,len,res,list);
			list.remove(list.size()-1);
			helper(nums,pos+1,len,res,list);
	}
}
	public static void main(String[] arg) {
		int[] nums= {1,2,3};
		Solution s = new Solution();
		List res=s.subsets(nums);
		//s.sum2(nums, 7);
		System.out.println(1);
	}
}

