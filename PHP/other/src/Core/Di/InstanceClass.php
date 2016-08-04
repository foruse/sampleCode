<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di;

/**
 * Class InstanceClass
 */
class InstanceClass
{
    /**
     * @var string
     */
    protected $id;

    /**
     * @var string
     */
    protected $class;

    /**
     * @var array
     */
    protected $arguments = [];

    /**
     * @var bool
     */
    protected $shared;

    /**
     * Constructor
     *
     * @param string $id
     * @param string $class
     * @param array $arguments
     * @param bool $shared
     */
    public function __construct($id, $class, array $arguments = [], $shared = false)
    {
        $this->id = $id;
        $this->class = $class;
        $this->arguments = $arguments;
        $this->shared = $shared;
    }

    /**
     * @return string
     */
    public function getId()
    {
        return $this->id;
    }

    /**
     * @return string
     */
    public function getClass()
    {
        return $this->class;
    }

    /**
     * @return array
     */
    public function getArguments()
    {
        return $this->arguments;
    }

    /**
     * @return bool
     */
    public function isShared()
    {
        return $this->shared;
    }
}
