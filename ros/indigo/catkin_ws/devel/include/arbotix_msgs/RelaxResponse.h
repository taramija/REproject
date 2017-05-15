// Generated by gencpp from file arbotix_msgs/RelaxResponse.msg
// DO NOT EDIT!


#ifndef ARBOTIX_MSGS_MESSAGE_RELAXRESPONSE_H
#define ARBOTIX_MSGS_MESSAGE_RELAXRESPONSE_H


#include <string>
#include <vector>
#include <map>

#include <ros/types.h>
#include <ros/serialization.h>
#include <ros/builtin_message_traits.h>
#include <ros/message_operations.h>


namespace arbotix_msgs
{
template <class ContainerAllocator>
struct RelaxResponse_
{
  typedef RelaxResponse_<ContainerAllocator> Type;

  RelaxResponse_()
    {
    }
  RelaxResponse_(const ContainerAllocator& _alloc)
    {
  (void)_alloc;
    }






  typedef boost::shared_ptr< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> > Ptr;
  typedef boost::shared_ptr< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> const> ConstPtr;

}; // struct RelaxResponse_

typedef ::arbotix_msgs::RelaxResponse_<std::allocator<void> > RelaxResponse;

typedef boost::shared_ptr< ::arbotix_msgs::RelaxResponse > RelaxResponsePtr;
typedef boost::shared_ptr< ::arbotix_msgs::RelaxResponse const> RelaxResponseConstPtr;

// constants requiring out of line definition



template<typename ContainerAllocator>
std::ostream& operator<<(std::ostream& s, const ::arbotix_msgs::RelaxResponse_<ContainerAllocator> & v)
{
ros::message_operations::Printer< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> >::stream(s, "", v);
return s;
}

} // namespace arbotix_msgs

namespace ros
{
namespace message_traits
{



// BOOLTRAITS {'IsFixedSize': True, 'IsMessage': True, 'HasHeader': False}
// {'arbotix_msgs': ['/home/turtlebot/ros/indigo/catkin_ws/src/arbotix_ros/arbotix_msgs/msg'], 'std_msgs': ['/opt/ros/indigo/share/std_msgs/cmake/../msg']}

// !!!!!!!!!!! ['__class__', '__delattr__', '__dict__', '__doc__', '__eq__', '__format__', '__getattribute__', '__hash__', '__init__', '__module__', '__ne__', '__new__', '__reduce__', '__reduce_ex__', '__repr__', '__setattr__', '__sizeof__', '__str__', '__subclasshook__', '__weakref__', '_parsed_fields', 'constants', 'fields', 'full_name', 'has_header', 'header_present', 'names', 'package', 'parsed_fields', 'short_name', 'text', 'types']




template <class ContainerAllocator>
struct IsFixedSize< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> >
  : TrueType
  { };

template <class ContainerAllocator>
struct IsFixedSize< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> const>
  : TrueType
  { };

template <class ContainerAllocator>
struct IsMessage< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> >
  : TrueType
  { };

template <class ContainerAllocator>
struct IsMessage< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> const>
  : TrueType
  { };

template <class ContainerAllocator>
struct HasHeader< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> >
  : FalseType
  { };

template <class ContainerAllocator>
struct HasHeader< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> const>
  : FalseType
  { };


template<class ContainerAllocator>
struct MD5Sum< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> >
{
  static const char* value()
  {
    return "d41d8cd98f00b204e9800998ecf8427e";
  }

  static const char* value(const ::arbotix_msgs::RelaxResponse_<ContainerAllocator>&) { return value(); }
  static const uint64_t static_value1 = 0xd41d8cd98f00b204ULL;
  static const uint64_t static_value2 = 0xe9800998ecf8427eULL;
};

template<class ContainerAllocator>
struct DataType< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> >
{
  static const char* value()
  {
    return "arbotix_msgs/RelaxResponse";
  }

  static const char* value(const ::arbotix_msgs::RelaxResponse_<ContainerAllocator>&) { return value(); }
};

template<class ContainerAllocator>
struct Definition< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> >
{
  static const char* value()
  {
    return "\n\
\n\
";
  }

  static const char* value(const ::arbotix_msgs::RelaxResponse_<ContainerAllocator>&) { return value(); }
};

} // namespace message_traits
} // namespace ros

namespace ros
{
namespace serialization
{

  template<class ContainerAllocator> struct Serializer< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> >
  {
    template<typename Stream, typename T> inline static void allInOne(Stream&, T)
    {}

    ROS_DECLARE_ALLINONE_SERIALIZER
  }; // struct RelaxResponse_

} // namespace serialization
} // namespace ros

namespace ros
{
namespace message_operations
{

template<class ContainerAllocator>
struct Printer< ::arbotix_msgs::RelaxResponse_<ContainerAllocator> >
{
  template<typename Stream> static void stream(Stream&, const std::string&, const ::arbotix_msgs::RelaxResponse_<ContainerAllocator>&)
  {}
};

} // namespace message_operations
} // namespace ros

#endif // ARBOTIX_MSGS_MESSAGE_RELAXRESPONSE_H
